/** This code is copyright Articulate Software (c) 2003.  Some portions
copyright Teknowledge (c) 2003 and reused under the terms of the GNU license.
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Teknowledge in any writings, briefings, publications, presentations, or
other representations of any software which incorporates, builds on, or uses this
code.  Please cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment,
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico. See also http://sigmakee.sourceforge.net

 This class expects the following to be in the ontology. Their absence won't cause an exception, but will prevent correct behavior.
   instance

 */

package com.articulate.sigma.nlg;

import com.articulate.sigma.*;
import com.articulate.sigma.utils.StringUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import tptp_parser.TPTPFormula;

import java.io.IOException;
import java.util.*;

/** ***************************************************************
 *  A class that handles the generation of natural language from logic.
 *
 *  @author Adam Pease - apease [at] articulatesoftware [dot] com, with thanks
 *  to Michal Sevcenko - sevcenko@vc.cvut.cz for development of the
 *  formatting language.
 */
public class LanguageFormatter {

    public static boolean debug = false;

    // a list of format parameters or words and the sentence words they match with
    public static Map<String,CoreLabel> outputMap = new HashMap<>();
    public static TPTPFormula tptpStep;
    private final String statement;

    private final Map<String, String> phraseMap;

    // kb.getTermFormatMap() for this language
    private final Map<String, String> termMap;

    private static KB kb;
    private final String language;

    private final Map<String, Set<String>> variableTypes;

    private final Map<String, Set<String>> variableToInstanceMap;

    // Modifiable versions of variableTypes and variableToInstanceMap for informal NLG.
    private Map<String, Set<String>> variableTypesNLG;
    private Map<String, Set<String>> variableToInstanceMapNLG;

    // FIXME: Verify that we handle all operators, then delete this field.
    private static final List<String> notReadyOperators = Lists.newArrayList();

    private static final String OLLAMA_HOST = "http://127.0.0.1:11434";

    private static OllamaClient ollamaClient = null;

    private RenderMode renderMode = RenderMode.HTML;

    enum RenderMode {
        HTML,
        TEXT
    }

    private enum Dir { P, N, QP, QN }

    /**
     * "Informal" NLG refers to natural language generation in which the formal logic terms are expressions are
     * eliminated--e.g. "a man drives" instead of "there exist a process and an agent such that the process is an instance of driving and
     * the agent is an instance of human and the agent is an agent of the process".
     */
    private boolean doInformalNLG = false;

    public static boolean paraphraseLLM = false;

    public void setDoInformalNLG(boolean doIt) {
        doInformalNLG = doIt;
    }

    private static final Map<String,String> LOGIC_DOCS;

    static {
        Map<String,String> m = new LinkedHashMap<>();

        m.put("=>",
                "Implication. (=> A B) means: if A is true, then B must be true. It does not assert A; it constrains cases where A holds.");
        m.put("and",
                "Conjunction. (and A B ...) means all listed subformulas are true.");
        m.put("or",
                "Disjunction. (or A B ...) means at least one listed subformula is true.");
        m.put("xor",
                "Exclusive-or. (xor A B) means exactly one of A or B is true, but not both.");
        m.put("<=>",
                "Biconditional (equivalence). (<=> A B) means A is true if and only if B is true.");
        m.put("not",
                "Negation. (not A) means A is false.");
        m.put("forall",
                "Universal quantification. (forall (?X ...) A) means A holds for all values of the variables.");
        m.put("exists",
                "Existential quantification. (exists (?X ...) A) means there exists at least one assignment of the variables making A true.");
        m.put("holds",
                "Temporal holding. (holds T P) means predicate/formula P holds at time/interval T. In SUMO this is commonly expressed as holdsDuring/holdsAt, but 'holds' is treated here as a temporal wrapper.");

        LOGIC_DOCS = Collections.unmodifiableMap(m);
    }

    /**
     * The stack holds information on the formula being processed, so that successive recursive calls can refer to it.
     */
    private final LanguageFormatterStack theStack = new LanguageFormatterStack();

    // define the class that is used simply as a key in a CoreLabel
    // The value will be a variable that corresponds to the token by
    // prepending a '?' to the token plus its index, or by appending
    // a '*' to the token
    public static class VariableAnnotation implements CoreAnnotation<String> {
        @Override
        public Class<String> getType() {
            return String.class;
        }
    }

    // define the class that is used simply as a key in a CoreLabel
    // The value is the number of the argument in a relation, corresponding
    // to this text token
    public static class RelationArgumentAnnotation implements CoreAnnotation<Integer> {
        @Override
        public Class<Integer> getType() {
            return Integer.class;
        }
    }

    private static final class Keywords {
        final String COMMA;
        final String IF;
        final String THEN;
        final String AND;
        final String OR;
        final String XOR;
        final String IFF;
        final String NOT;
        final String FORALL;
        final String EXISTS;
        final String EXIST;
        final String NOTEXIST;
        final String NOTEXISTS;
        final String HOLDS;
        final String SOTHAT;
        final String SUCHTHAT;

        Keywords(String language) {
            this.COMMA = NLGUtils.getKeyword(",", language);
            this.IF = NLGUtils.getKeyword("if", language);
            this.THEN = NLGUtils.getKeyword("then", language);
            this.AND = NLGUtils.getKeyword(Formula.AND, language);
            this.OR = NLGUtils.getKeyword(Formula.OR, language);

            // Formula.XOR may be empty; use same fallback you used elsewhere.
            String xorKey = StringUtil.isNonEmptyString(Formula.XOR) ? Formula.XOR : "xor";
            this.XOR = NLGUtils.getKeyword(xorKey, language);
            this.IFF = NLGUtils.getKeyword("if and only if", language);
            this.NOT = NLGUtils.getKeyword(Formula.NOT, language);
            this.FORALL = NLGUtils.getKeyword("for all", language);
            this.EXISTS = NLGUtils.getKeyword("there exists", language);
            this.EXIST = NLGUtils.getKeyword("there exist", language);
            this.NOTEXIST = NLGUtils.getKeyword("there don't exist", language);
            this.NOTEXISTS = NLGUtils.getKeyword("there doesn't exist", language);
            this.HOLDS = NLGUtils.getKeyword("holds", language);
            this.SOTHAT = NLGUtils.getKeyword("so that", language);

            String such = NLGUtils.getKeyword("such that", language);
            this.SUCHTHAT = StringUtil.emptyString(such) ? this.SOTHAT : such;
        }
    }

    /*******************************************************************************
     * @param stmt The statement to be formatted.
     * @param phraseMap kb.getFormatMap() for this language
     * @param termMap kb.getTermFormatMap() for this language
     * @param kb
     * @param language
     */
    public LanguageFormatter(String stmt, Map<String, String> phraseMap, Map<String, String> termMap,
                             KB kb, String language) {

        outputMap = new HashMap<>();
        this.statement = stmt;
        this.phraseMap = phraseMap;
        this.termMap = termMap;
        LanguageFormatter.kb = kb;
        this.language = language;

        Formula f = new Formula();
        f.read(statement);
        FormulaPreprocessor fp = new FormulaPreprocessor();
        variableTypes = fp.computeVariableTypes(f, kb);
        variableToInstanceMap = fp.findExplicitTypes(kb,f);
        init();
    }

    /***********************************************************************************
     */
    private void init() {

        // Get special versions of variable maps.
        variableToInstanceMapNLG = new HashMap<>(variableToInstanceMap);
        variableTypesNLG = new HashMap<>();

        String variable;
        Set<String> origInstances;
        Set<String> newInstances;
        String newStr;
        // variableToInstanceMapNLG should map variables to the correct surface form of the SUMO term
        for (Map.Entry<String, Set<String>> entry : variableToInstanceMapNLG.entrySet()) {
            variable = entry.getKey();
            origInstances = entry.getValue();
            newInstances = Sets.newHashSet();
            for (String instance : origInstances) {
                newStr = SumoProcessCollector.getProperFormOfEntity(instance, kb);
                newInstances.add(newStr);
            }
            variableToInstanceMapNLG.put(variable, newInstances);
        }

        // variableTypes should map variables to the correct surface form of the SUMO term, but we want it to contain
        // only those terms not in variableToInstanceMapNLG
        for (Map.Entry<String, Set<String>> entry : variableTypes.entrySet()) {
            variable = entry.getKey();
            if (! variableToInstanceMapNLG.containsKey(variable)) {
                origInstances = entry.getValue();
                newInstances = Sets.newHashSet();
                for (String instance : origInstances) {
                    newStr = SumoProcessCollector.getProperFormOfEntity(instance, kb);
                    newInstances.add(newStr);
                }
                variableTypesNLG.put(variable, newInstances);
            }
        }
    }

    /***********************************************************************************
     * Hyperlink terms in a natural language format string.  This assumes that
     * terms to be hyperlinked are in the form &%termName$termString , where
     * termName is the name of the term to be browsed in the knowledge base and
     * termString is the text that should be displayed hyperlinked.
     *
     * @param href the anchor string up to the term= parameter, which this method
     *               will fill in.
     * @return
     */
    public String htmlParaphrase(String href) {

        init();
        String nlFormat = "";
        try {
            theStack.pushNew();
            StackElement element = theStack.getCurrStackElement();
            String template = paraphraseStatement(statement, false, false, 1);

            // Check for successful informal NLG of the entire statement.
            if (doInformalNLG)  {
                String informalNLG = theStack.doStatementLevelNatlLanguageGeneration();
                if (! informalNLG.isEmpty())    {
                    // Resolve variables.
                    informalNLG = LanguageFormatter.variableReplace(informalNLG, variableToInstanceMapNLG, variableTypesNLG, kb, language);
                    if (debug) System.out.println("LanguageFormatter.htmlParaphrase():  " + informalNLG);
                    template = informalNLG;
                }
            }
            theStack.pop(element);

            // Replace any variables in the template.
            if (StringUtil.isNonEmptyString(template)) {
                Formula f = new Formula();
                f.read(statement);
                FormulaPreprocessor fp = new FormulaPreprocessor();
                //HashMap varMap = fp.computeVariableTypes(kb);
                Map<String, Set<String>> instanceMap = new HashMap<>();
                Map<String, Set<String>> classMap = new HashMap<>();
                Map<String, Set<String>> types = fp.computeVariableTypes(f, kb);
                Iterator<String> it = types.keySet().iterator();
                String var;
                Set<String> typeList;
                Iterator<String> it2;
                String t;
                Set<String> values;
                while (it.hasNext()) {
                    var = it.next();
                    typeList = types.get(var);
                    it2 = typeList.iterator();
                    while (it2.hasNext()) {
                        t = it2.next();
                        if (t.endsWith("+")) {
                            values = new HashSet<>();
                            if (classMap.containsKey(var))
                                values = classMap.get(var);
                            values.add(t.substring(0, t.length()-1));
                            classMap.put(var, values);
                        }
                        else {
                            values = new HashSet<>();
                            if (instanceMap.containsKey(var))
                                values = instanceMap.get(var);
                            values.add(t);
                            instanceMap.put(var, values);
                        }
                    }
                }
                if (!instanceMap.isEmpty() || !classMap.isEmpty() ) {
                    //if ((instanceMap != null && !instanceMap.isEmpty()) || (classMap != null && !classMap.isEmpty()))
                    template = variableReplace(template, instanceMap, classMap, kb, language);
                    if (debug) System.out.println("LanguageFormatter.htmlParaphrase(): template: " + template);
                }
                // Get rid of the percentage signs.
                nlFormat = NLGUtils.resolveFormatSpecifiers(template, href);
                if (debug) System.out.println("LanguageFormatter.htmlParaphrase(): nlFormat: " + nlFormat);

            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }


        // nlFormat = paraphrased with links!
        // e.g <a href="http://localhost:8080/sigma/Browse.jsp?lang=EnglishLanguage&kb=SUMO&term=instrument">instrument</a> is an <a href="http://localhost:8080/sigma/Browse.jsp?lang=EnglishLanguage&kb=SUMO&term=instance">instance</a> of <a href="http://localhost:8080/sigma/Browse.jsp?lang=EnglishLanguage&kb=SUMO&term=Relation">relation</a>

        if (paraphraseLLM){
            nlFormat = paraphraseWithLLM(nlFormat);
        }

        return nlFormat;
    }

    /******************************************************************
     * Create a natural language paraphrase of a logical statement.
     *
     * @param stmt The statement to be paraphrased.
     * @param isNegMode Whether the statement is negated.
     * @param depth An int indicating the level of nesting, for control of indentation.
     * @return A String, which is the paraphrased statement.
     */
    public String paraphraseStatement(String stmt, boolean isNegMode, boolean isQuestionMode, int depth) {

        if (debug) System.out.println("INFO in LanguageFormatter.paraphraseStatement(): stmt: " + stmt);
        if (Formula.empty(stmt)) {
            System.err.println("Error in LanguageFormatter.paraphraseStatement(): stmt is empty");
            return "";
        }
        boolean alreadyTried = kb.getLoadFormatMapsAttempted().contains(language);
        if ((phraseMap == null) || phraseMap.isEmpty()) {
            if (!alreadyTried) { kb.loadFormatMaps(language); }
            return "";
        }
        if ((termMap == null) || termMap.isEmpty()) {
            if (!alreadyTried) { kb.loadFormatMaps(language); }
            return "";
        }
        StringBuilder result = new StringBuilder();
        String ans;
        Formula f = new Formula(stmt);

        theStack.insertFormulaArgs(f);
        if (f.atom()) {
            ans = processAtom(stmt, termMap);
            return ans;
        }
        else {
            if (!f.listP()) {
                if (!StringUtil.emptyString(stmt))
                    System.err.println("Error in LanguageFormatter.paraphraseStatement(): "
                        + " Statement is not an atom or a list: "
                        + stmt);
                return "";
            }
        }
        // The test immediately below should be changed to check that
        // the car (predicate) is either an atomic constant, or a
        // non-atomic (list) term formed with the function
        // PredicateFn.
        String pred = f.car();
        handleCaseRole(f, pred, isNegMode);

        // Mark the predicate as processed.
        // FIXME: handle other predicates here: located, orientation, etc.
        if (pred.equals("instance")) {
            // Do not mark as processed "complicated" clauses containing functions. They will have to be handled later in the process.
            // FIXME: The check below handles "(instance ?hamburger (FoodForFn Human))", but not "(instance (GovernmentFn ?Place) StateGovernment))".
            if (!kb.isFunction(new Formula(f.complexArgumentsToArrayList(2).get(0)).car())) {
                theStack.translateCurrProcessInstantiation(kb, f);
                theStack.markFormulaArgAsProcessed(stmt);
            }
        }

        if (!Formula.atom(pred)) {
            System.err.println("Error in LanguageFormatter.paraphraseStatement(): statement "
                    + stmt
                    + " has a formula in the predicate position.");
            return stmt;
        }

        if (NLGUtils.logicalOperator(pred)) {
            ans = paraphraseLogicalOperator(stmt, isNegMode, depth+1);
            List<String> translations = theStack.getCurrStackFormulaArgs();
            String translation = "";
            if (translations.size() > 1) {
                // "Plug" the operator into the informal language, e.g. "=> A B" becomes "if A, then B".
                translation = generateFormalNaturalLanguage(translations, pred, isNegMode);
            }
            else if (translations.size() == 1)
                translation = translations.get(0);
            if (! translation.isEmpty())
                theStack.getCurrStackElement().setTranslation(translation, true);
            return ans;
        }

        if (phraseMap.containsKey(pred)) {
            if (pred.equals("attribute"))    {
                theStack.markFormulaArgAsProcessed(stmt);
                // Add the property to the current stack element.
                SumoProcessEntityProperty property = new SumoProcessEntityProperty(f);
                String key = f.cdrAsFormula().car();
                theStack.addToCurrProperties(key, property);
                // Modify the variable maps so that the variables are mapped to a surface form for the entity which includes
                // the attribute.
                updateVariables(variableToInstanceMapNLG, key, property);
                updateVariables(variableTypesNLG, key, property);
            }
            else if (pred.equals("names")) {
                theStack.markFormulaArgAsProcessed(stmt);
                // Get the name, with the quotation marks which surround it.
                Formula forumulaCdr = f.cdrAsFormula();
                String name = forumulaCdr.car();
                // Get the variable.
                String var = forumulaCdr.cdr().substring(1, forumulaCdr.cdr().length() - 1);
                // Replace all variables with the name.
                variableToInstanceMapNLG.put(var, Sets.newHashSet(name));
                // TODO: not needed? variableTypesNLG.put(var, Sets.newHashSet(name));
            }
            ans = paraphraseWithFormat(stmt, isNegMode, isQuestionMode);
            return ans;
        }
        else {                              // predicate has no paraphrase
            if (Formula.isVariable(pred))
                result.append(pred);
            else
                result.append(processAtom(pred, termMap));
            f.read(f.cdr());
            String arg;
            while (!f.empty()) {
                if (debug) System.out.println("INFO in LanguageFormatter.paraphraseStatement(): stmt: " + f);
                if (debug) System.out.println("length: " + f.listLength());
                if (debug) System.out.println("result: " + result);
                arg = f.car();
                f.read(f.cdr());
                result.append(Formula.SPACE);
                if (Formula.atom(arg))
                    result.append(processAtom(arg, termMap));
                else
                    result.append(paraphraseStatement(arg, isNegMode, isQuestionMode, depth + 1));
                if (!f.empty()) {
                    if (f.listLength() > 1)
                        result.append(", ");
                    else
                        result.append(" and");
                }
            }
        }
        ans = result.toString();
        return ans;
    }


    private String unescapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\n", "\n")
                .replace("\\t", "\t")
                .replace("\\r", "\r")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }


    private String buildGlossaryFromMaps(Set<String> symbols) {

        StringBuilder sb = new StringBuilder();
        for (String sym : symbols) {
            String gloss = null;

            // termMap/phraseMap are already in the LanguageFormatter instance
            if (termMap != null) gloss = termMap.get(sym);
            if (gloss == null && phraseMap != null) gloss = phraseMap.get(sym);

            if (StringUtil.isNonEmptyString(gloss)) {
                sb.append(sym).append(" = ").append(gloss).append("\n");
            }
        }
        return sb.toString();
    }

    private Set<String> extractSymbolsFromKif(String kif) {

        Set<String> logops = new HashSet<>(Arrays.asList(
                "if","then","=>","and","or","xor","<=>","not","forall","exists","holds"
        ));

        if (StringUtil.emptyString(kif)) return Collections.singleton("");

        Set<String> out = new LinkedHashSet<>();
        // Space out punctuation/operators so they tokenize cleanly
        String cleaned = kif
                .replace("(", " ")
                .replace(")", " ")
                .replace("\n", " ")
                .replace("\r", " ")
                .replace("\t", " ")
                .replace("=>", " => ")
                .replace("<=>", " <=> ");

        for (String tok : cleaned.split("\\s+")) {
            if (tok.isEmpty()) continue;
            if (tok.startsWith("?")) continue; // variables

            // Accept logical operators explicitly
            if (logops.contains(tok)) {
                out.add(tok);
                continue;
            }

            // Accept standard SUMO symbols
            if (tok.matches("[A-Za-z][A-Za-z0-9_+\\-]*")) {
                out.add(tok);
            }
        }
        return out;
    }

    private String normalizeSigmaMarkup(String doc) {
        if (StringUtil.emptyString(doc)) return doc;

        // Remove Sigma markup like &%Term
        return doc.replaceAll("&%([A-Za-z0-9_+-]+)", "$1");
    }


    private String callOllamaJson(String prompt) {

        String model = "llama3.2";
//        String model = "qwen2.5:14b-instruct";

        String ollamaHost = KBmanager.getMgr().getPref("ollamaHost");
        if (StringUtil.emptyString(ollamaHost)) ollamaHost = OLLAMA_HOST;

        if (ollamaClient == null) ollamaClient = new OllamaClient(ollamaHost);

        Map<String,Object> opts = new HashMap<>();
        opts.put("temperature", 0);
        opts.put("top_p", 1);
        opts.put("num_predict", 500);

        boolean jsonMode = true;

        try {
            if (model != null && model.startsWith("gpt-oss")) {
                return ollamaClient.chat(model, prompt, opts, jsonMode);
            }
            opts.put("seed", 0);
            return ollamaClient.generate(model, prompt, opts, jsonMode);
        } catch (IOException e) {
            System.out.println("ERROR | callOllamaJson: " + e);
            return "{}";
        }
    }


    private String jsonExtractField(String json, String field) {
        if (json == null) return "";
        String key = "\"" + field + "\"";
        int k = json.indexOf(key);
        if (k < 0) return "";
        int colon = json.indexOf(':', k);
        if (colon < 0) return "";

        int q1 = json.indexOf('"', colon + 1);
        if (q1 < 0) return "";

        boolean esc = false;
        for (int i = q1 + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (esc) { esc = false; continue; }
            if (c == '\\') { esc = true; continue; }
            if (c == '"') return unescapeJson(json.substring(q1 + 1, i));
        }
        return "";
    }


    private String paraphraseWithLLM(String currentHtmlOrTemplate) {

        // We will ask the LLM to explain the *SUO-KIF* (this.statement),
        // while still allowing it to produce fluent English.
        final String stepKif = (this.statement != null) ? this.statement : "";
        final String cleanedTemplate = removeLinks(currentHtmlOrTemplate);

        Set<String> symbols = extractSymbolsFromKif(stepKif);

        String documentationBlock = buildDocumentationFromMaps(symbols);

        // Build a small glossary grounded in Sigma maps (termFormat/format equivalents).
        String glossaryBlock = buildGlossaryFromMaps(symbols);

        String prompt1 =
                "### SYSTEM ROLE:\n"
                        + "You are an English rewriter.\n"
                        + "Your sole job is to rewrite the given TEMPLATE into clear, grammatical, natural English.\n"
                        + "Do NOT use any external knowledge. Do NOT interpret SUMO. Do NOT infer new meaning.\n\n"

                        + "### HARD CONSTRAINTS:\n"
                        + "1. PRESERVE LOGICAL SKELETON: Keep the same logical connectives already present in the TEMPLATE.\n"
                        + "   - Keep all occurrences of: \"if\", \"then\", \"and\", \"or\", \"either\", \"not\", \"for all\", \"there exists\".\n"
                        + "   - Do NOT change \"and\" into \"or\" or vice versa.\n"
                        + "   - Do NOT introduce \"either/or\" unless the TEMPLATE already contains \"or\".\n"
                        + "2. NO NEW FACTS: Do not add information not stated in the TEMPLATE.\n"
                        + "3. NO SYMBOLS: Do not use logical symbols (¬, ∨, →, =>). Use plain English words only.\n"
                        + "4. ENTITY CONSISTENCY: Do not merge or split entities. If the TEMPLATE mentions X and Y, keep them distinct.\n\n"

                        + "### OUTPUT FORMAT:\n"
                        + "Return valid JSON ONLY. No preamble.\n"
                        + "Schema: {\"paraphrase\":\"...\"}\n\n"

                        + "### INPUT:\n"
                        + "TEMPLATE:\n"
                        + cleanedTemplate + "\n\n"

                        + "### INSTRUCTION:\n"
                        + "Rewrite the TEMPLATE now and output the JSON:";

        long start = System.nanoTime();
        String json1 = callOllamaJson(prompt1);
        long end = System.nanoTime();
        double seconds = (end - start) / 1_000_000_000.0;

        String paraphrase1 = jsonExtractField(json1, "paraphrase");

        System.out.println("===================== Ollama Call  ===================");
        System.out.println("\nSUO-KIF: "+stepKif);
        System.out.println("\nGlossary block: "+glossaryBlock);
        System.out.println("\nDocumentation block: "+documentationBlock);
        System.out.println("\nCleaned Template: "+cleanedTemplate);
        System.out.println("\nParaphrase: "+paraphrase1);
        System.out.printf("\nOllama call took %.3f seconds%n%n", seconds);

        if (StringUtil.emptyString(paraphrase1)) {
            // Fallback: if JSON failed, return whatever we got
            return StringUtil.isNonEmptyString(json1) ? json1 : cleanedTemplate;
        }


        // SKIP second pass for now, because it adds a lot of time to the execution.
//        // Verifier pass: keep meaning tight
//        String prompt2 =
//                "You are a strict SUO-KIF paraphrase verifier.\n"
//                        + "Compare STEP and PARAPHRASE for structural equivalence.\n"
//                        + "For each check, output one of: PASS, FAIL, or NA (not applicable).\n"
//                        + "A check is NA if STEP does not contain that construct.\n\n"
//
//                        + "CHECKS:\n"
//                        + "Q: Quantifiers (forall/exists) variables and scopes preserved.\n"
//                        + "N: Negation (not) scope preserved.\n"
//                        + "I: Conditionals (=>) and nesting preserved.\n"
//                        + "B: Boolean structure (and/or) groupings preserved.\n"
//                        + "P: Predicates preserved: same predicate names and same argument order.\n"
//                        + "X: No extra predicates/facts introduced.\n"
//                        + "S: Symbols preserved exactly (variables/constants names unchanged).\n\n"
//
//                        + "OUTPUT VALID JSON ONLY with this schema:\n"
//                        + "{"
//                        + "\"verdict\":\"PASS|FAIL\","
//                        + "\"checks\":{\"Q\":\"PASS|FAIL|NA\",\"N\":\"PASS|FAIL|NA\",\"I\":\"PASS|FAIL|NA\",\"B\":\"PASS|FAIL|NA\",\"P\":\"PASS|FAIL|NA\",\"X\":\"PASS|FAIL|NA\",\"S\":\"PASS|FAIL|NA\"},"
//                        + "\"issues\":[\"...\"],"
//                        + "\"corrected_paraphrase\":\"...\""
//                        + "}\n\n"
//
//                        + "RULES:\n"
//                        + "- verdict is FAIL if ANY check is FAIL.\n"
//                        + "- If verdict is PASS: corrected_paraphrase MUST equal PARAPHRASE exactly.\n"
//                        + "- If verdict is FAIL: corrected_paraphrase MUST be corrected.\n"
//                        + "- Keep SUMO terms and symbols unchanged.\n"
//                        + "- Do not add facts.\n\n"
//
//                        + "STEP (SUO-KIF):\n" + stepKif + "\n\n"
//                        + "PARAPHRASE:\n" + paraphrase1;
//
//
//        System.out.println("\n\n----------- PROMPT 2 ------------------");
//        System.out.println(prompt2);
//
//        String json2 = callOllamaJson(prompt2);
//        System.out.println("------Answer 2------");
//        String verdict = jsonExtractField(json2, "verdict");
//        String corrected = jsonExtractField(json2, "corrected_paraphrase");
//        System.out.println(json2);
//        System.out.println("----------------------------------------\n\n");
//        if ("FAIL".equalsIgnoreCase(verdict) && StringUtil.isNonEmptyString(corrected)) {
//            return corrected;
//        }
        return paraphrase1;
    }

    private String buildDocumentationFromMaps(Set<String> symbols) {
        StringBuilder sb = new StringBuilder();

        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));

        for (String sym : symbols) {

            // 1) Built-in docs for logical operators / keywords
            String doc = LOGIC_DOCS.get(sym);

            // 2) Fallback to SUMO KB docs for domain predicates/classes
            if (doc == null) {
                String gloss = KButilities.getDocumentation(kb, sym);
                doc = normalizeSigmaMarkup(gloss);
            }

            if (StringUtil.isNonEmptyString(doc)) {
                sb.append(sym).append(" = \"").append(doc.trim()).append("\"").append("\n");
            }
        }
        return sb.toString();
    }



    public static boolean checkOllamaHealth(){
        String ollamaHost = KBmanager.getMgr().getPref("ollamaHost");
        if (StringUtil.emptyString(ollamaHost)) ollamaHost = OLLAMA_HOST;
        OllamaClient oc = new OllamaClient(ollamaHost, 1000, 1500); // short timeouts
        return oc.isHealthy();
    }


    private String removeLinks(String initialText){
        if (initialText == null) return null;
        // Replace <a ...> and </a> tags with nothing, keeping only the inner text
        return initialText.replaceAll("<a[^>]*>", "")   // remove opening <a ...>
                .replaceAll("</a>", "")       // remove closing </a>
                .trim();
    }

    /**
     * Generate a natural language summary of a proof using Ollama LLM
     * @param proofSteps List of proof steps as strings
     * @return HTML formatted proof summary
     */
    public static String generateProofSummary(List<String> proofSteps) {
        if (proofSteps == null || proofSteps.isEmpty()) {
            return "";
        }
        
        // Check if Ollama is available
        if (!checkOllamaHealth()) {
            return "<div style='color:#666; font-style:italic;'>Proof summary unavailable (Ollama is offline)</div>";
        }
        
        // Prepare the proof steps as a string
        StringBuilder proofText = new StringBuilder();
        for (int i = 0; i < proofSteps.size(); i++) {
            String step = proofSteps.get(i);
            // Clean HTML tags and links from the proof step
            step = step.replaceAll("<[^>]*>", "").trim();
            if (!step.isEmpty()) {
                proofText.append("Step ").append(i + 1).append(": ").append(step).append("\n");
            }
        }

        String cleanedText = proofText.toString()
        .replaceAll("forall|exists|instance|=>|<=", "")
        .replaceAll("[()]", "")
        .replaceAll("\\?\\w+", "")
        .replaceAll("\\s{2,}", " ")
        .trim();
       // System.out.println("LLM Summary Input Length: " + cleanedText.length());
        // System.out.println("Proof Step Count: " + proofSteps.size());
        
        // prompt for Ollama
        String prompt = "You are an expert instructor who explains formal proofs in clear, structured English.\n"
            + "Your task is to turn the proof steps below into a concise explanation that mirrors the style "
            + "of a logic textbook\n\n"
            + "Follow these guidelines carefully:\n"
            + "1. Begin by identifying the goal of the proof or the key claim being established.\n"
            + "2. Describe the initial assumption(s) used in the argument.\n"
            + "3. Explain how the reasoning unfolds, highlighting important implications, case analyses, or contradictions.\n"
            + "4. Never mention step numbers, variable names, or logical symbols from the original proof.\n"
            + "   Do NOT include symbols such as 'forall', 'exists', '=>', or object labels.\n"
            + "5. Refer to elements generically, using phrases like 'an entity', 'an object', or 'a relation'.\n"
            + "6. give variable names like 'x', 'y', or 'z' to represent generic entities where needed to avoid confusion.\n"
            + "7. Use natural narrative transitions, such as:\n"
            + "     - 'The proof begins by assuming that…'\n"
            + "     - 'It then considers what must follow…'\n"
            + "     - 'From this, it becomes clear that…'\n"
            + "     - 'This leads to the conclusion that…'\n"
            + "8. Write 3–6 sentences that form one smooth, cohesive paragraph.\n"
            + "9. End with a clear statement of what the proof ultimately establishes.\n"
            + "10. Highlight the intuition behind *why* the conclusion must be true—go beyond mechanical steps.\n"
            + "   Explain the reasoning as if teaching a student who is new to logic.\n\n"
            + "Here are the proof steps:\n"
            + cleanedText
            + "\n\n"
            + "Now write the explanation in the style and tone of a standard proof textbook:"
            + "ensure that a non-technical reader can follow the logic.\n";

 
        String model = "llama3.2";
        String ollamaHost = KBmanager.getMgr().getPref("ollamaHost");
        if (StringUtil.emptyString(ollamaHost)) ollamaHost = OLLAMA_HOST;
        
        OllamaClient ollama = new OllamaClient(ollamaHost, 10_000, 60_000); // longer timeouts for proof summarization
        
        try {
            String summary = ollama.generate(model, prompt);
            if (!StringUtil.emptyString(summary)) {
                // Format the summary in a nice HTML box
                return "<div style='background:#f8f9fa; border:1px solid #dee2e6; border-radius:4px; "
                     + "padding:12px; margin:15px 0;'>"
                     + "<h4 style='margin:0 0 8px 0; color:#495057;'>Proof Summary</h4>"
                     + "<p style='margin:0; line-height:1.6; color:#212529;'>" 
                     + summary + "</p></div>";
            }
        } catch (IOException e) {
            System.out.println("ERROR | LanguageFormatter | generateProofSummary: " + e);
            return "<div style='color:#b00;'>Error generating proof summary: " + e.getMessage() + "</div>";
        }
        
        return "";
    }

    /*****************************************************************
     * Modify the given variable map so that given key is mapped to a surface form for the entity which includes
     * the given property.
     * @param variableMap
     * @param key
     * @param property
     */
    private void updateVariables(Map<String, Set<String>> variableMap, String key, SumoProcessEntityProperty property) {

        if (variableMap.containsKey(key))     {
            Set<String> oldSet = variableMap.get(key);
            Set<String> newSet = Sets.newHashSet();
            String newStr;
            for (String oldStr : oldSet)   {
                // modify each noun accordingly
                newStr = property.getSurfaceFormForNoun(oldStr, kb);
                newSet.add(newStr);
            }
            variableMap.put(key, newSet);
        }
    }

    /*****************************************************************
     * Insert the give case role into the current element's process map; or, if the SumoProcess already exists
     * there, update it with the new information.
     * @param formula
     * @param caseRole
     * @param isNegMode
     */
    private void handleCaseRole(Formula formula, String caseRole, boolean isNegMode) {

        if (! doInformalNLG)
            return;
        if (kb.kbCache.isInstanceOf(caseRole, "CaseRole")) {
            try {
                if (!theStack.isEmpty()) {
                    String caseArgument = formula.cadr() + Formula.SPACE + formula.caddr();
                    String[] caseArgs = caseArgument.trim().split(Formula.SPACE);
                    String processInstanceName = caseArgs[0];
                    String processParticipant = caseArgs[1];

                    if (variableTypes.containsKey(processInstanceName)) {

                        Set<String> vals = variableTypes.get(processInstanceName);
                        if (NLGUtils.containsProcess(vals, kb))  {
                            // Mark the argument as PROCESSED.
                            theStack.markFormulaArgAsProcessed(formula.getFormula());
                            // Get the data in the stack's topmost element.
                            Map<String, SumoProcessCollector> thisProcessMap = theStack.getCurrProcessMap();
                            if (thisProcessMap.containsKey(processInstanceName)) {
                                // Already there. Add new information if any exists.
                                SumoProcessCollector sProcess = thisProcessMap.get(processInstanceName);
                                //String resolvedParticipant = resolveVariable(processParticipant);
                                sProcess.addRole(caseRole, processParticipant);
                                if (isNegMode)
                                    sProcess.setPolarity(VerbProperties.Polarity.NEGATIVE);
                            }
                            else {
                                // Insert into stack.
                                String resolvedProcessName = resolveVariable(processInstanceName);
                                //String resolvedParticipant = resolveVariable(processParticipant);
                                SumoProcessCollector sProcess = new SumoProcessCollector(kb, caseRole, resolvedProcessName, processParticipant);
                                thisProcessMap.put(processInstanceName, sProcess);
                                if (isNegMode)
                                    sProcess.setPolarity(VerbProperties.Polarity.NEGATIVE);
                            }
                        }
                    }

                }
            }
            catch (IllegalArgumentException e) {
                // Recover from exception by turning off full NLG.
                this.doInformalNLG = false;
                String temp = statement.replaceAll("\\s+", Formula.SPACE);    // clean up, reducing consecutive whitespace to single space
                String msg = "Handled IllegalArgumentException after finding case role.\n   Exception message:\n      " + e.getMessage() + "\n" +
                        "   Formula:\n       " + temp + "\n";
                System.out.println("LanguageFormatter " + msg);
            }
        }
    }

    /** ***************************************************************
     * Uses variableToInstanceMap and variableTypes to resolve a given variable.
     * If more than one entry is in the map, currently takes the first one returned by the iterator.
     * @param input
     * @return
     */
    private String resolveVariable(String input) {

        String retVal = input;
        if (variableToInstanceMap.containsKey(input))    {
            Set<String> values = variableToInstanceMap.get(input);
            // If more than one entry, take the first one returned by the iterator.
            retVal = values.iterator().next();
        }
        else if (variableTypes.containsKey(input)) {
            Set<String> values = variableTypes.get(input);
            // If more than one entry, take the first one returned by the iterator.
            retVal = values.iterator().next();
        }
        return retVal;
    }

    /** ***************************************************************
     * Create a natural language paraphrase for statements involving the logical operators.
     * @param stmt The logical statement for which we want to paraphrase the operator, arg 0.
     * @param isNegMode Is the expression negated?
     * @param depth The nested operator depth, for controlling indentation.
     * @return The natural language paraphrase as a String, or null if the predicate was not a logical operator.
     */
    private String paraphraseLogicalOperator(String stmt, boolean isNegMode, int depth) {

        try {
            if (NLGUtils.getKeywordMap() == null) {
                if (debug) System.err.println("Error in LanguageFormatter.paraphraseLogicalOperator(): " + "keywordMap is null");
                return null;
            }
            List<String> args = new ArrayList<>();
            Formula f = new Formula();
            f.read(stmt);
            String pred = f.getStringArgument(0);
            f.read(f.cdr());

            String ans;

            if (LanguageFormatter.notReadyOperators.contains(pred))   {
                doInformalNLG = false;
            }

            // Push new element onto the stack.
            theStack.pushNew();
            StackElement inElement = theStack.getCurrStackElement();

            if (pred.equals(Formula.NOT)) {
                theStack.setPolarity(VerbProperties.Polarity.NEGATIVE);
                ans = paraphraseStatement(f.car(), true, false,depth + 1);
                inElement.setProcessPolarity(VerbProperties.Polarity.NEGATIVE);
                theStack.pushCurrSumoProcessDown();

                if (theStack.getCurrStackElement().getTranslated()) {
                    theStack.pushTranslationDownToNotLevel(stmt);
                    theStack.pop(inElement);
                }
                else    {
                    theStack.pop(inElement);
                    theStack.markFormulaArgAsProcessed(stmt);
                }
                return ans;
            }
            try {
                String arg, result;
                while (!f.empty()) {
                    arg = f.car();
                    result = paraphraseStatement(arg, false, false,depth + 1);
                    if (StringUtil.isNonEmptyString(result))    {
                        args.add(result);
                        // Let the next element down in the stack know whether we've performed informal NLG.
                        theStack.pushCurrTranslatedStateDown(arg);
                    }
                    else {
                        System.out.println("INFO in LanguageFormatter.paraphraseLogicalOperator(): "
                                + "bad result for \"" + arg + "\": " + result);
                        return "";
                    }
                    f.read(f.cdr());
                }
                // Perform natural language output.
                if (doInformalNLG) {
                    Map<String, SumoProcessCollector> thisProcessMap = theStack.getCurrProcessMap();
                    if (thisProcessMap != null) {
                        if (! thisProcessMap.isEmpty()) {
                            // Only perform NLG if the args list has been fully processed.
                            boolean doIt = theStack.areFormulaArgsProcessed();
                            if (doIt) {
                                // Try to do natural language generation on top element of stack.
                                String output = theStack.doProcessLevelNatlLanguageGeneration();
                                if (!output.isEmpty())
                                    theStack.getPrevStackElement().setTranslation(output, true);
                            }
                        }
                    }
                }
            }
            catch (IllegalArgumentException ex) {
                ex.printStackTrace();
            }
            finally {
                // Pop the stack.
                theStack.pop(inElement);
            }
            theStack.pushCurrTranslatedStateDown(stmt);
            // If this clause has successfully been generated into informal language, then we can drop any remaining quantifiers and mark
            // the entire clause as translated.
            theStack.setCurrTranslatedIfQuantified();
            // Do formal NLG in case informal NLG fails.
            return generateFormalNaturalLanguage(args, pred, isNegMode);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return "";
    }


    // Overload for backward compatibility
    public String generateFormalNaturalLanguage(List<String> args,
                                                String pred,
                                                boolean isNegMode) {
        return generateFormalNaturalLanguage(args, pred, isNegMode, renderMode);
    }

    /** ***************************************************************
     */
    public String generateFormalNaturalLanguage(List<String> args, String pred, boolean isNegMode, RenderMode mode) {

        if (args.isEmpty()) return "";

        if (mode == null) mode = RenderMode.HTML;

        Keywords k = new Keywords(language);

        // Local helper to avoid repeating translateWord(...) everywhere.
        List<String> tArgs = new ArrayList<>(args.size());
        for (int i = 0; i < args.size(); i++) {
            tArgs.add(maybeTranslateArg(args.get(i)));
        }

        if (pred.equals(Formula.IF)) {
            if (isNegMode) {
                return tArgs.get(0) + Formula.SPACE + k.AND + Formula.SPACE + negateClause(tArgs.get(1));
            } else {
                if (mode == RenderMode.HTML) {
                    // Special handling for Arabic.
                    boolean isArabic = isArabicLanguage(language);
                    StringBuilder sb = new StringBuilder();
                    sb.append("<ul><li>");
                    if (isArabic) sb.append("<span dir=\"rtl\">");
                    sb.append(k.IF).append(Formula.SPACE).append(tArgs.get(0)).append(k.COMMA);
                    if (isArabic) sb.append("</span>");
                    sb.append("</li><li>");
                    if (isArabic) sb.append("<span dir=\"rtl\">");
                    sb.append(k.THEN).append(Formula.SPACE).append(tArgs.get(1));
                    if (isArabic) sb.append("</span>");
                    sb.append("</li></ul>");
                    return sb.toString();
                }
                // TEXT
                return k.IF + Formula.SPACE + tArgs.get(0) + k.COMMA + Formula.SPACE + k.THEN + Formula.SPACE + tArgs.get(1);
            }
        }

        if (pred.equalsIgnoreCase(Formula.AND)) {
            if (isNegMode) {
                List<String> negated = new ArrayList<>(tArgs.size());
                for (int i = 0; i < tArgs.size(); i++) {
                    negated.add(negateClause(tArgs.get(i)));
                }
                return join(negated, k.OR);
            }
            return join(tArgs, k.AND);
        }

        if (pred.equalsIgnoreCase("holds")) {
            // Keep existing behavior (odd chaining preserved).
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < tArgs.size(); i++) {
                if (i > 0) {
                    if (isNegMode) {
                        sb.append(Formula.SPACE).append(k.NOT);
                    }
                    sb.append(Formula.SPACE).append(k.HOLDS).append(Formula.SPACE);
                }
                sb.append(tArgs.get(i));
            }
            return sb.toString();
        }


        if (pred.equalsIgnoreCase(Formula.OR)) {
            // Note: current neg-mode behavior flips the joiner but does NOT negate operands.
            if (isNegMode) {
                List<String> negated = new ArrayList<>(tArgs.size());
                for (int i = 0; i < tArgs.size(); i++) {
                    negated.add(negateClause(tArgs.get(i)));
                }
                return join(negated, k.AND);
            }
            return join(tArgs, k.OR);
        }

        if (pred.equalsIgnoreCase(Formula.XOR)) {
            // Keep existing behavior (joins with XOR);
            return renderXor(tArgs);
        }

        if (pred.equals(Formula.IFF)) {
            if (isNegMode) {
                return renderXor(tArgs);
            }
            return tArgs.get(0) + Formula.SPACE + k.IFF + Formula.SPACE + tArgs.get(1);
        }

        if (pred.equalsIgnoreCase(Formula.UQUANT)) {
            StringBuilder sb = new StringBuilder();
            if (isNegMode) {
                sb.append(k.NOT).append(Formula.SPACE);
            }
            sb.append(k.FORALL).append(Formula.SPACE);

            String vars = args.get(0);
            if (vars.contains(Formula.SPACE)) {
                sb.append(translateWord(termMap, NLGUtils.formatList(vars, language)));
            } else {
                sb.append(translateWord(termMap, vars));
            }

            sb.append(Formula.SPACE).append(tArgs.get(1));
            return sb.toString();
        }


        if (pred.equalsIgnoreCase(Formula.EQUANT)) {
            StringBuilder sb = new StringBuilder();
            String vars = args.get(0);

            if (vars.contains(Formula.SPACE)) {
                sb.append(isNegMode ? k.NOTEXIST : k.EXIST)
                        .append(Formula.SPACE)
                        .append(translateWord(termMap, NLGUtils.formatList(vars, language)));
            } else {
                sb.append(isNegMode ? k.NOTEXISTS : k.EXISTS)
                        .append(Formula.SPACE)
                        .append(translateWord(termMap, vars));
            }

            sb.append(Formula.SPACE)
                    .append(k.SUCHTHAT)
                    .append(Formula.SPACE)
                    .append(tArgs.get(1));

            return sb.toString();
        }
        return "";
    }

    private String negateClause(String s) {
        return "~{ " + s + " }";
    }

    private boolean isArabicLanguage(String lang) {
        return lang != null &&
                (lang.matches(".*(?i)arabic.*") || lang.equalsIgnoreCase("ar"));
    }

    private String join(List<String> parts, String op) {
        return String.join(Formula.SPACE + op + Formula.SPACE, parts);
    }

    /** User-friendly XOR for 2 args; fallback to AND/OR expansion for others. */
    private String renderXor(List<String> parts) {

        Keywords k = new Keywords(language);

        if (parts == null || parts.isEmpty()) return "";
        if (parts.size() == 1) return parts.get(0);

        if (parts.size() == 2) {
            String either = NLGUtils.getKeyword("either", language);
            String butNotBoth = NLGUtils.getKeyword("but not both", language);
            if (StringUtil.emptyString(either)) either = "either";
            if (StringUtil.emptyString(butNotBoth)) butNotBoth = "but not both";

            return either + Formula.SPACE + parts.get(0)
                    + Formula.SPACE + k.OR + Formula.SPACE + parts.get(1)
                    + k.COMMA + Formula.SPACE + butNotBoth;
        }

        // n-ary: parity (odd number true) in DNF.
        // Complexity: O(n * 2^(n-1)) clauses. Fine for small n (typical here).
        List<String> disjuncts = new ArrayList<>();

        int n = parts.size();
        int maskMax = 1 << n;
        for (int mask = 0; mask < maskMax; mask++) {
            if ((Integer.bitCount(mask) % 2) == 1) { // odd parity
                List<String> conj = new ArrayList<>(n);
                for (int i = 0; i < n; i++) {
                    boolean isTrue = ((mask >> i) & 1) == 1;
                    String lit = parts.get(i);
                    conj.add(isTrue ? lit : negateClause(lit));
                }
                disjuncts.add(join(conj, k.AND));
            }
        }
        return join(disjuncts, k.OR);
    }

    /** Args reaching generateFormalNaturalLanguage() are usually already paraphrased.
     * Only translate when the arg looks like a raw atomic token.
     */
    private String maybeTranslateArg(String s) {
        if (StringUtil.emptyString(s)) return s;

        // If it looks like a composed clause or already-rendered markup, do NOT translate.
        if (s.indexOf(' ') >= 0) return s;          // multiword / composed
        if (s.contains("&%")) return s;             // Sigma hyperlink marker
        if (s.contains("~{")) return s;             // negation wrapper
        if (s.contains("<")) return s;              // HTML fragment
        if (s.startsWith("\"") && s.endsWith("\"")) return s; // quoted string

        // Otherwise it's plausibly a raw atom; translate once.
        return translateWord(termMap, s);
    }

    /** True if s already looks like Sigma's annotated term placeholder: &%TERM$"label" */
    private static boolean isAnnotatedTerm(String s) {
        if (StringUtil.emptyString(s)) return false;
        // Minimal and fast check (no regex): must start with &% and contain $" and end with "
        return s.startsWith("&%") && s.contains("$\"") && s.endsWith("\"");
    }

    /** ***************************************************************
     * Create a natural language paraphrase of a logical statement, where the
     * predicate is not a logical operator.  Use a printf-like format string to generate
     * the paraphrase.
     * @param stmt the statement to format
     * @param isNegMode whether the statement is negated, and therefore requiring special formatting.
     * @return the paraphrased statement.
     */
    private String paraphraseWithFormat(String stmt, boolean isNegMode, boolean isQuestionMode) {

        if (debug) System.out.println("INFO in LanguageFormatter.paraphraseWithFormat(): Statement: " + stmt);

        Formula f = new Formula();
        f.read(stmt);
        String pred = f.car();
        String strFormat = phraseMap.get(pred);

        if (strFormat.contains("&%"))                    // setup the term hyperlink
            strFormat = strFormat.replaceAll("&%(\\w+)","&%" + pred + "\\$\"$1\"");

        // Apply directive selection/cleanup deterministically
        strFormat = applyDirectives(strFormat, isNegMode, isQuestionMode);

        //System.out.println("INFO in LanguageFormatter.paraphraseWithFormat(): 3 format: " + strFormat);
        if (strFormat.contains("%*"))
            strFormat = NLGUtils.expandStar(f, strFormat, language);

        //System.out.println("INFO in LanguageFormatter.paraphraseWithFormat(): 3.5 format: " + strFormat);
        int num = 1;                                          // handle arguments
        String argPointer = ("%" + num);
        String arg;
        String para;
        while (strFormat.contains(argPointer)) {
            if (debug) System.out.println("INFO in LanguageFormatter.paraphraseWithFormat(): Statement: " + f.getFormula());
            if (debug) System.out.println("arg: " + f.getArgument(num));
            if (debug) System.out.println("num: " + num);
            if (debug) System.out.println("str: " + strFormat);
            arg = f.getStringArgument(num);
            if (Formula.isVariable(arg))
                para = arg;
            else
                para = paraphraseStatement(arg, isNegMode, isQuestionMode, 1);
            if (debug) System.out.println("para: " + para);
            //outputMap.add(new AVPair(pred + "-" + argPointer, para));
            //System.out.println("para: " + para);
            //if (!Formula.atom(para)) {
                // Add the hyperlink placeholder for arg.
           //     if (Formula.isVariable(argPointer))
           //         strFormat = strFormat.replace(argPointer, para);
            //    else {
                    /**
                     List splitPara = Arrays.asList(para.split("\\s+"));
                     System.out.println("splitPara == " + splitPara);
                     StringBuilder pb = new StringBuilder();
                     int spLen = splitPara.size();
                     for (int i = 0; i < spLen; i++) {
                     if (i > 0) {
                     pb.append(Formula.SPACE);
                     }
                     pb.append("&%");
                     pb.append(arg);
                     pb.append("$");
                     pb.append((String) splitPara.get(i));
                     }
                     */
            //        strFormat = strFormat.replace(argPointer, para);
            //    }
            //}
            //else
            strFormat = strFormat.replace(argPointer,para);
            CoreLabel cl = new CoreLabel();
            cl.setValue(para);
            cl.set(RelationArgumentAnnotation.class,num);
            outputMap.put(para,cl);
            num++;
            argPointer = ("%" + num);
        }
        return strFormat;
    }

    private Dir chooseDir(boolean isNegMode, boolean isQuestionMode) {
        if (isQuestionMode) return isNegMode ? Dir.QN : Dir.QP;
        return isNegMode ? Dir.N : Dir.P;
    }

    private static Dir parseDir(String tag) {
        if ("p".equals(tag)) return Dir.P;
        if ("n".equals(tag)) return Dir.N;
        if ("qp".equals(tag)) return Dir.QP;
        if ("qn".equals(tag)) return Dir.QN;
        return null;
    }

    /** Selects the active directive blocks (%p/%n/%qp/%qn) and removes the rest deterministically. */
    private String applyDirectives(String fmt, boolean isNegMode, boolean isQuestionMode) {

        if (StringUtil.emptyString(fmt)) return fmt;

        Dir keep = chooseDir(isNegMode, isQuestionMode);
        StringBuilder out = new StringBuilder(fmt.length());

        int i = 0;
        while (i < fmt.length()) {

            int pct = fmt.indexOf('%', i);
            if (pct < 0) {
                out.append(fmt.substring(i));
                break;
            }

            // copy everything before %
            out.append(fmt.substring(i, pct));
            i = pct;

            // Try to parse directive token: %p{...} / %n{...} / %qp{...} / %qn{...}
            int j = i + 1;
            if (j >= fmt.length()) {
                out.append('%');
                break;
            }

            // Read tag (letters only)
            int tagStart = j;
            while (j < fmt.length() && Character.isLetter(fmt.charAt(j))) j++;
            String tag = fmt.substring(tagStart, j);
            Dir dir = parseDir(tag);

            // Not a known directive -> keep '%' and continue
            if (dir == null) {
                out.append('%');
                i = i + 1;
                continue;
            }

            // If next char is '{', we have a braced directive block
            if (j < fmt.length() && fmt.charAt(j) == '{') {
                int bodyStart = j + 1;
                int bodyEnd = fmt.indexOf('}', bodyStart);

                // If no closing brace, treat as literal text (preserve old behavior best-effort)
                if (bodyEnd < 0) {
                    out.append(fmt.substring(i, j + 1)); // includes "%tag{"
                    i = j + 1;
                    continue;
                }

                String body = fmt.substring(bodyStart, bodyEnd);

                // Keep only the chosen directive content; drop others
                if (dir == keep) {
                    out.append(body);
                }

                i = bodyEnd + 1;
                continue;
            }

            // Unbraced directives (%n / %p) were previously used as NOT keyword insertion.
            // Preserve existing behavior: only %n is meaningful (negation keyword injection),
            // and only when neg-mode and not question-mode.
            if ("n".equals(tag)) {
                if (isNegMode && !isQuestionMode) {
                    out.append(NLGUtils.getKeyword(Formula.NOT, language));
                }
            }
            // %p without braces is a no-op historically; drop it.

            i = j;
        }

        // Normalize whitespace a bit (keep conservative to avoid behavior drift)
        return out.toString().replaceAll("\\s+", Formula.SPACE).trim();
    }


    /** ***************************************************************
     * Process an atom into an appropriate NL string.  If a URL, add
     * spaces for readability.  Return variable unaltered.  Add
     * term format string to all other atoms.
     */
     static String processAtom(String atom, Map<String, String> termMap) {

        if (StringUtil.emptyString(atom))
            return atom;

        // If already annotated, do nothing (idempotent).
        if (isAnnotatedTerm(atom))
            return atom;

        String unquoted = StringUtil.removeEnclosingQuotes(atom);

        // Numbers: keep raw
        boolean isNumber;
        try {
            Double dbl = Double.valueOf(unquoted);
            isNumber = !dbl.isNaN();
        }catch (NumberFormatException nex) {
            isNumber = false;
        }
        if (isNumber) return atom;

        if (StringUtil.isQuotedString(atom)) {
            return NLGUtils.formatLongUrl(atom);
        }

        // Variables and digit strings: keep raw
        if (Formula.isVariable(atom)) return atom;

        if (StringUtil.isDigitString(unquoted)) return atom;

        // Otherwise: ontology term (or constant) -> annotated consistently
        String label = atom;
        if (termMap != null) {
            String mapped = termMap.get(atom);
            if (StringUtil.isNonEmptyString(mapped))
                label = mapped;
        }
        return "&%" + atom + "$\"" + label + "\"";
    }


    /** ***************************************************************
     * Return the NL format of an individual word.
     */
    public static String translateWord(Map<String,String> termMap, String word) {

        String ans = word;
        try {
            if (!Formula.isVariable(word) && (termMap != null)) {
                String pph = termMap.get(word);
                if (StringUtil.isNonEmptyString(pph))
                    ans = pph;
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** ***************************************************************
     * Remove the type declarations from the text
     */
    public static String removePreamble(String input) {

        if (input.contains("such that"))
            input = input.substring(input.indexOf("such that") + 10,input.length());
        return input;
    }

    /** ***************************************************************
     * create a map from numbered tokens to their types
     */
    private static void createObjectMap(String form) {

        form = removePreamble(form);
        form = form + Formula.SPACE;
        if (debug) System.out.println("LanguageFormatter.createObjectMap(): input " + form);
        int tokenNum = 1;
//        boolean inQuote;
        int index = 0;
        StringBuilder termBuffer = new StringBuilder();
        CoreLabel cl;
        int phraseEnd;
        String phrase;
        int argnum;
        String[] phraseWords;
        while (index < form.length()) {
            //System.out.println(index);
            switch (form.charAt(index)) {
                case '"':
                    System.err.println("Error in LanguageFormatter.createObjectMap(): premature quote at index " +
                            index + " in " + form);
                    return;
                case ' ':
                    cl = new CoreLabel();
                    cl.setOriginalText(termBuffer.toString());
                    cl.setValue(termBuffer.toString());
                    cl.setIndex(tokenNum);
                    if (debug) System.out.println("LanguageFormatter.createObjectMap(): CoreLabel: " + cl);
                    if (debug) System.out.println("LanguageFormatter.createObjectMap(): termBuffer: " + termBuffer);
                    outputMap.put(termBuffer.toString(),cl);
                    termBuffer = new StringBuilder();
                    tokenNum++;
                    index++;
                    break;
                case '&':
                    index++;
                    if (form.charAt(index) != '%') {
                        System.err.println("Error in LanguageFormatter.createObjectMap(): bad symbol " +
                                form.charAt(index) + " in " + form);
                        return;
                    }
                    index++;
                    int termEnd = form.indexOf("$",index);
                    if (termEnd == -1) {
                        System.err.println("Error in LanguageFormatter.createObjectMap(): missing $ in " +
                                form + " after " + index);
                        return;
                    }
                    String term = form.substring(index,termEnd);
                    int quote1 = termEnd + 1;
                    if (form.charAt(quote1) != '"') {
                        System.err.println("Error in LanguageFormatter.createObjectMap(): bad symbol " +
                                form.charAt(quote1) +" at index " + quote1 + " in " + form);
                        return;
                    }
                    int phraseStart = quote1 + 1;
                    phraseEnd = form.indexOf("\"",phraseStart);
                    phrase = form.substring(phraseStart,phraseEnd);
                    phrase = StringUtil.removeDoubleSpaces(phrase);
                    argnum = -1;
                    if (Character.isDigit(form.charAt(phraseEnd+1)))
                        argnum = Integer.parseInt(Character.toString(form.charAt(phraseEnd+1)));
                    phraseWords = phrase.split(Formula.SPACE);
                    for (String s : phraseWords) {
                        cl = new CoreLabel();
                        cl.setOriginalText(s);
                        cl.setValue(s);
                        cl.setIndex(tokenNum);
                        cl.set(RelationArgumentAnnotation.class,argnum);
                        if (!kb.isInstanceOf(term,"Relation"))
                            cl.setCategory(term);
                        else
                            if (debug) System.out.println("LanguageFormatter.createObjectMap(): " + term + " is a relation");
                        if (debug) System.out.println("LanguageFormatter.createObjectMap(): argnum: " +  cl.get(RelationArgumentAnnotation.class));
                        if (debug) System.out.println("LanguageFormatter.createObjectMap(): category: " + term);
                        outputMap.put(cl.toString(),cl);
                        if (debug) System.out.println("LanguageFormatter.createObjectMap(): CoreLabel: " + cl.toString());
                        //outputMap.put(s + "-" + Integer.toString(tokenNum),term);
                        tokenNum++;
                    }
                    index = phraseEnd + 1;
                    if (argnum != -1)
                        index++;
                    if (index < form.length()-1 && form.charAt(index) == ' ')
                        index++;
                    break;
                default:
                    termBuffer.append(form.charAt(index));
                    index++;
                    break;
            }
        }
        if (debug) System.out.println("LanguageFormatter.createObjectMap(): result " + outputMap);
    }

    /** **************************************************************
     * Replace variables with types, and articles appropriate to how many times
     * they have occurred.
     *
     * There is a known bug where variables that are a substring of each other
     * causes problems.
     * @param typeMap is a map with variable type keys that counts
     *                how many times variables of the given type
     *                appears in the paraphrase.  This is done in
     *                order to state "an entity, another entity, a
     *                third entity" etc.
     */
    private static String incrementalVarReplace(String form, String varString, String varType,
                                                String varPretty, String language,
                                                boolean isClass, Map<String, Integer> typeMap) {

        String argNumStr = "";
        if (debug) System.out.println("LanguageFormatter.incrementalVarReplace(): form " + form);
        if (debug) System.out.println("LanguageFormatter.incrementalVarReplace(): varString " + varString);
        if (debug) System.out.println("LanguageFormatter.incrementalVarReplace(): varType " + varType);
        if (debug) System.out.println("LanguageFormatter.incrementalVarReplace(): varPretty " + varPretty);

        if (outputMap.keySet().contains(varString)) {
            CoreLabel cl = outputMap.get(varString);
            cl.setOriginalText(varPretty);
            cl.setValue(varPretty);
            if (debug) System.out.println("LanguageFormatter.incrementalVarReplace(): arg num " +
                    cl.get(RelationArgumentAnnotation.class));
            argNumStr = Integer.toString(cl.get(RelationArgumentAnnotation.class));
            outputMap.put(varString, cl); // create a "dummy" CoreLabel to hold the variable value
        }

        if (!form.contains(Formula.V_PREF)) // if there are variables, the replacements are not done yet
            createObjectMap(form);

        String result = form;

        // Make necessary changes if the variable is a quoted string, i.e. a name.
        if (varPretty == null && varType.matches("\".*\"")) {
            String name = varType.substring(1, varType.length() - 1);
            // Avoid substring collisions here too
            String nameRegex = "\\?" + Pattern.quote(varString.substring(1)) + "(?![A-Za-z0-9_])";
            result = result.replaceAll(nameRegex, Matcher.quoteReplacement(name));
            if (debug) System.out.println("LanguageFormatter.incrementalVarReplace(): result " + result);
            return result;
        }

        boolean isArabic = (language.matches(".*(?i)arabic.*") || language.equalsIgnoreCase("ar"));
        if (StringUtil.emptyString(varPretty))
            varPretty = varType;

        boolean found = true;
        int occurrenceCounter = 1;
        if (typeMap.keySet().contains(varType)) {
            occurrenceCounter = typeMap.get(varType);
            occurrenceCounter++;
            typeMap.put(varType, occurrenceCounter);
        }
        else
            typeMap.put(varType, 1);

        int count = 1;
        String article;
        String replacement;
        String defArt;

        // Key fix: only match the exact variable token (don't replace ?S inside ?SP)
        final String varRegex = "\\?" + Pattern.quote(varString.substring(1)) + "(?![A-Za-z0-9_])";

        while (found) {
            if (result.contains(varString) && count < 20) {
                if (isClass) {
                    article = NLGUtils.getArticle("kind", count, occurrenceCounter, language);
                    replacement = (article + Formula.SPACE + NLGUtils.getKeyword("kind of", language)
                            + Formula.SPACE + varPretty);
                    if (isArabic)
                        replacement = (NLGUtils.getKeyword("kind of", language) + Formula.SPACE + varPretty);

                    result = result.replaceFirst(
                            varRegex,
                            Matcher.quoteReplacement("&%" + varType + "$\"" + replacement + "\"" + argNumStr)
                    );
                }
                else {
                    article = NLGUtils.getArticle(varPretty, count, occurrenceCounter, language);
                    replacement = (article + Formula.SPACE + varPretty);
                    if (isArabic) {
                        defArt = NLGUtils.getKeyword("the", language);
                        if (article.startsWith(defArt) && !varPretty.startsWith(defArt)) {
                            // This has to be refined to insert shadda for sun letters.
                            varPretty = (defArt + varPretty);
                        }
                        replacement = (varPretty + Formula.SPACE + article);
                    }

                    result = result.replaceFirst(
                            varRegex,
                            Matcher.quoteReplacement("&%" + varType + "$\"" + replacement + "\"" + argNumStr)
                    );
                }
            }
            else
                found = false;
            count++;
        }

        if (debug) System.out.println("LanguageFormatter.incrementalVarReplace(): result (2) " + result);
        return result;
    }

    /** **************************************************************
     * Replace variables in a formula with paraphrases expressing their
     * type.
     */
    public static String variableReplace(String form, Map<String, Set<String>> instMap,
            Map<String, Set<String>> classMap, KB kb, String language) {

        String result = form;
        Map<String,Integer> typeMap = new HashMap<>();
        List<String> varList = NLGUtils.collectOrderedVariables(form);
        Iterator<String> it = varList.iterator();
        String varString;
        Set<String> instanceArray;
        Set<String> subclassArray;
        String varType;
        String varPretty;
        while (it.hasNext()) {
            varString = it.next();
            if (StringUtil.isNonEmptyString(varString)) {
                instanceArray = instMap.get(varString);
                subclassArray = classMap.get(varString);

                if (subclassArray != null && !subclassArray.isEmpty()) {
                    varType = (String) subclassArray.toArray()[0];
                    varPretty = kb.getTermFormatMap(language).get(varType);
                    result = incrementalVarReplace(result, varString, varType, varPretty, language, true, typeMap);
                }
                else {
                    if (instanceArray != null && !instanceArray.isEmpty()) {
                        varType = (String) instanceArray.toArray()[0];
                        varPretty = kb.getTermFormatMap(language).get(varType);
                        result = incrementalVarReplace(result, varString, varType, varPretty, language, false, typeMap);
                    }
                    else {
                        varPretty = kb.getTermFormatMap(language).get("Entity");
                        if (StringUtil.emptyString(varPretty))
                            varPretty = "entity";
                        result = incrementalVarReplace(result, varString, "Entity", varPretty, language, false, typeMap);
                    }
                }
            }
        }
        if (debug) System.out.println("LanguageFormatter.variableReplace(): " + result);
        return result;
    }

    /** **************************************************************
     */
    public static void test1() {

        try {
            KBmanager.getMgr().initializeOnce();
        }
        catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));

        // INFO in LanguageFormatter.paraphraseLogicalOperator(): bad result for
        String stmt =  "(and (instance ?GUIE1 GUIElement) (hasGUEState ?GUIE1 GUE_ActiveState)" +
                " (properPart ?GUIE1 ?GUIE2) (instance ?GUIE2 GUIElement))";
        Formula f = new Formula(stmt);
        System.out.println("Formula: " + f.getFormula());
        System.out.println("result: " + StringUtil.filterHtml(NLGUtils.htmlParaphrase("", stmt, kb.getFormatMap("EnglishLanguage"), kb.getTermFormatMap("EnglishLanguage"), kb, "EnglishLanguage")));
        System.out.println();
    }

    /** **************************************************************
     */
    public static void test2() {

        try {
            KBmanager.getMgr().initializeOnce();
        }
        catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));

        String stmt = "(exists (?D ?H)\n" +
                "   (and\n" +
                "       (instance ?D Driving)\n" +
                "       (instance ?H Human)\n" +
                "       (agent ?D ?H)))";
        Formula f = new Formula(stmt);
        System.out.println("Formula: " + f.getFormula());
        System.out.println("result: " + StringUtil.filterHtml(NLGUtils.htmlParaphrase("", stmt, kb.getFormatMap("EnglishLanguage"), kb.getTermFormatMap("EnglishLanguage"), kb, "EnglishLanguage")));
        //LanguageFormatter lf = new LanguageFormatter(stmt, kb.getFormatMap("EnglishLanguage"),
        //        kb.getTermFormatMap("EnglishLanguage"),kb,"EnglishLanguage");
        //System.out.println(lf.paraphraseStatement(stmt,false,false,0));
        System.out.println();

        stmt = "(exists (?D ?H)\n" +
                "   (and\n" +
                "       (instance ?D Driving)\n" +
                "       (instance ?H Human)\n" +
                "       (names ?H \"John\")\n" +
                "       (agent ?D ?H)))";
        f = new Formula();
        f.read(stmt);
        System.out.println("Formula: " + f.getFormula());
        System.out.println("result: " + StringUtil.filterHtml(NLGUtils.htmlParaphrase("", stmt, kb.getFormatMap("EnglishLanguage"), kb.getTermFormatMap("EnglishLanguage"), kb, "EnglishLanguage")));
        System.out.println();

        stmt = "(exists (?D ?H ?Car)\n" +
                "   (and\n" +
                "       (instance ?D Driving)\n" +
                "       (instance ?H Human)\n" +
                "       (names ?H \"John\")\n" +
                "       (instance ?Car Automobile)\n" +
                "       (agent ?D ?H)\n" +
                "       (patient ?D ?Car)))";
        f = new Formula();
        f.read(stmt);
        System.out.println("Formula: " + f.getFormula());
        System.out.println("result: " + StringUtil.filterHtml(NLGUtils.htmlParaphrase("", stmt, kb.getFormatMap("EnglishLanguage"), kb.getTermFormatMap("EnglishLanguage"), kb, "EnglishLanguage")));
        System.out.println();

        stmt = "(exists (?D ?H ?C ?A)\n" +
                "   (and\n" +
                "       (instance ?D Driving)\n" +
                "       (instance ?H Human)\n" +
                "       (names ?H \"John\")\n" +
                "       (instance ?C Automobile)\n" +
                "       (instance ?A Airport)\n" +
                "       (agent ?D ?H)\n" +
                "       (destination ?D ?A)\n" +
                "       (patient ?D ?C)))";
        f = new Formula();
        f.read(stmt);
        System.out.println("Formula: " + f.getFormula());
        System.out.println("result: " + StringUtil.filterHtml(NLGUtils.htmlParaphrase("", stmt, kb.getFormatMap("EnglishLanguage"), kb.getTermFormatMap("EnglishLanguage"), kb, "EnglishLanguage")));
        System.out.println();
    }

    /** **************************************************************
     */
    public static void test3() {

        try {
            KBmanager.getMgr().initializeOnce();
        }
        catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
        debug = true;
        // INFO in LanguageFormatter.paraphraseLogicalOperator(): bad result for
        String stmt =  "(exists (?FINANCIALTRANSACTION1 ?AGENT2 ) (broker ?FINANCIALTRANSACTION1 ?AGENT2 ))";
        Formula f = new Formula(stmt);
        System.out.println("Formula: " + f.getFormula());
        System.out.println("result: " + StringUtil.filterHtml(NLGUtils.htmlParaphrase("", stmt, kb.getFormatMap("EnglishLanguage"), kb.getTermFormatMap("EnglishLanguage"), kb, "EnglishLanguage")));
        System.out.println(NLGUtils.outputMap);
        System.out.println();

        stmt =  "(exists (?MOTION1 ?OBJECT2 ) (moves ?MOTION1 ?OBJECT2 ))";
        f = new Formula(stmt);
        System.out.println("Formula: " + f.getFormula());
        System.out.println("result: " + StringUtil.filterHtml(NLGUtils.htmlParaphrase("", stmt, kb.getFormatMap("EnglishLanguage"), kb.getTermFormatMap("EnglishLanguage"), kb, "EnglishLanguage")));
        System.out.println(NLGUtils.outputMap);
        System.out.println();
    }

    /** **************************************************************
     */
    public static void test4() {

        try {
            KBmanager.getMgr().initializeOnce();
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
        debug = true;
        // INFO in LanguageFormatter.paraphraseLogicalOperator(): bad result for
        String stmt = "(or (not (subclass Human Object)) (not (instance Human Class)) spl6_2)";
        Formula f = new Formula(stmt);
        System.out.println("Formula: " + f.getFormula());
        System.out.println("result: " +
                StringUtil.filterHtml(NLGUtils.htmlParaphrase("", stmt, kb.getFormatMap("EnglishLanguage"),
                        kb.getTermFormatMap("EnglishLanguage"), kb, "EnglishLanguage")));
        System.out.println(NLGUtils.outputMap);
        System.out.println();
    }

    /** ***************************************************************
     */
    public static void setKB(KB kbin) {

        kb = kbin;
    }

    /** ***************************************************************
     * generate English paraphrase
     */
    public static String toEnglish(String form) {

        return StringUtil.filterHtml(NLGUtils.htmlParaphrase("", form, kb.getFormatMap("EnglishLanguage"),
                kb.getTermFormatMap("EnglishLanguage"), kb, "EnglishLanguage"));
    }

    /** ***************************************************************
     */
    public static void showHelp() {

        System.out.println("Language generation ");
        System.out.println("  options:");
        System.out.println("  -h - show this help screen");
        System.out.println("  -t - run test");
        System.out.println("  -g \"<formula>\" - generate English from formula");
    }

    /** ***************************************************************
     * A test method.
     */
    public static void main(String[] args) {

        System.out.println("INFO in " + LanguageFormatter.class.getName() + " .main()");
        if (args != null && args.length > 1 && args[0].equals("-h")) {
            showHelp();
        }
        else if (args.length > 1 && args[0].equals("-g")) {
            KBmanager.getMgr().initializeOnce();
            Formula f = new Formula(args[1]);
            kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
            System.out.println("translation for\n" + f);
            String actual = toEnglish(StringUtil.removeEnclosingQuotes(args[1]));
            System.out.println(StringUtil.filterHtml(actual));
        }
        else {
            test4();
        }
    }
}

