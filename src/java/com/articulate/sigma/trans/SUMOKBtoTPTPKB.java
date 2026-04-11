package com.articulate.sigma.trans;

import com.articulate.sigma.*;
import com.articulate.sigma.parsing.Expr;
import com.articulate.sigma.parsing.ExprToTFF;
import com.articulate.sigma.parsing.ExprToTPTP;
import com.articulate.sigma.parsing.FormulaAST;
import com.articulate.sigma.parsing.SuokifVisitor;
import com.articulate.sigma.utils.StringUtil;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.regex.Pattern;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SUMOKBtoTPTPKB {

    public static final boolean FILTER_SIMPLE_ONLY = false;

    private static final Pattern QUOTED_CALL_PATTERN =
            Pattern.compile(".*'[a-z][a-zA-Z0-9_]*\\(.*");

    public KB kb;

    // flags to support including numbers and HOL in pseudo-FOL for flexible provers
    public static boolean removeHOL = true; // remove higher order expressions
    public static boolean removeNum = true; // remove numbers
    public static boolean removeStrings = true;

    /** Flag to enable rapid parsing via multiple threads coordinated by an ExecutorService */
    public static boolean rapidParsing = true;

    public static boolean debug = false;

    // One-shot diagnostic flags (print first occurrence only, thread-safe)
    private static final java.util.concurrent.atomic.AtomicBoolean loggedReparse =
            new java.util.concurrent.atomic.AtomicBoolean(false);
    private static final java.util.concurrent.atomic.AtomicBoolean loggedExprPath =
            new java.util.concurrent.atomic.AtomicBoolean(false);
    private static final java.util.concurrent.atomic.AtomicBoolean loggedStringPath =
            new java.util.concurrent.atomic.AtomicBoolean(false);

    // Per-language path counters for observability (reset via resetPathCounters())
    public static final java.util.concurrent.atomic.AtomicInteger fofExprCount   = new java.util.concurrent.atomic.AtomicInteger(0);
    public static final java.util.concurrent.atomic.AtomicInteger fofStringCount = new java.util.concurrent.atomic.AtomicInteger(0);
    public static final java.util.concurrent.atomic.AtomicInteger tffExprCount   = new java.util.concurrent.atomic.AtomicInteger(0);
    public static final java.util.concurrent.atomic.AtomicInteger tffStringCount = new java.util.concurrent.atomic.AtomicInteger(0);

    public static void resetPathCounters() {
        fofExprCount.set(0); fofStringCount.set(0);
        tffExprCount.set(0); tffStringCount.set(0);
    }

    public static void logPathCounters() {
        System.out.println("SUMOKBtoTPTPKB path counters: " +
            "fof-expr=" + fofExprCount + " fof-string=" + fofStringCount + " | " +
            "tff-expr=" + tffExprCount + " tff-string=" + tffStringCount);
    }

    // ThreadLocal to allow parallel FOF/TFF generation
    private static final ThreadLocal<String> langTL = ThreadLocal.withInitial(() -> "fof");
    public static String getLang() { return langTL.get(); }
    public static void setLang(String l) { langTL.set(l); }
    /** Remove ThreadLocal values to prevent leaks in thread pools */
    public static void clearThreadLocal() { langTL.remove(); }

    public static boolean CWA = false;  // implement the closed world assumption

    public static Set<String> excludedPredicates = new HashSet<>();

    // maps TPTP axiom IDs to SUMO formulas
    public static Map<String,Formula> axiomKey = new HashMap<>();

    /**
     * Thread-local redirect for {@link #axiomKey} writes.  When set (non-null),
     * {@link #putAxiom} writes to this map instead of the global {@code axiomKey}.
     * Used by session-specific TPTP generation (via
     * {@link TPTPGenerationManager#generateFOFToPath}) to avoid overwriting the
     * global map that tracks shared base-KB axiom names.
     */
    static final ThreadLocal<Map<String,Formula>> localAxiomKeyOverride = new ThreadLocal<>();

    /**
     * Records a TPTP axiom name → KIF Formula mapping.
     * Writes to the thread-local override when one is active (session-specific generation),
     * otherwise writes to the global {@link #axiomKey}.
     */
    private void putAxiom(String name, Formula f) {
        Map<String,Formula> target = localAxiomKeyOverride.get();
        if (target != null) target.put(name, f);
        else axiomKey.put(name, f);
    }

    public Set<String> alreadyWrittenTPTPs = new HashSet<>();

    /** Progress bar text capture */
    private final StringBuilder progressSb = new StringBuilder();

    /** *************************************************************
     */
    public SUMOKBtoTPTPKB() {
        buildExcludedPredicates();
    }

    /** *************************************************************
     * define a set of predicates which will not be used for inference
     */
    public static Set<String> buildExcludedPredicates() {

        excludedPredicates.add("documentation");
        excludedPredicates.add("domain");
        excludedPredicates.add("format");
        excludedPredicates.add("termFormat");
        excludedPredicates.add("externalImage");
        excludedPredicates.add("relatedExternalConcept");
        excludedPredicates.add("relatedInternalConcept");
        excludedPredicates.add("formerName");
        excludedPredicates.add("abbreviation");
        excludedPredicates.add("conventionalShortName");
        excludedPredicates.add("conventionalLongName");

        return excludedPredicates;
    }

    /** *************************************************************
     * Given the set of terms whose KBcache entries changed (returned by an
     * incremental update method such as {@code addSubclass}), identify
     * all formulas in the KB that need to be retranslated to TPTP.
     *
     * <p>A formula is <em>affected</em> if any of the following holds:
     * <ol>
     *   <li><b>Direct reference</b> — the formula mentions any changed term in
     *       argument positions 0–5 (queried via the {@code KB.formulas} index).</li>
     *   <li><b>Predicate variable</b> — the formula has predicate variables
     *       ({@code predVarCache} is non-null and non-empty) <em>and</em> at least
     *       one changed term is a relation.  Such formulas are re-expanded by
     *       {@code PredVarInst} against the updated predicate set.</li>
     * </ol>
     *
     * <p>All affected formulas have their {@code varTypeCache} cleared so that
     * {@code computeVariableTypes()} recomputes fresh type-guards on the next
     * {@code preProcess()} call.
     *
     * @param kb           the knowledge base
     * @param changedTerms the terms returned by an incremental update
     * @return formulas that require retranslation (never null, may be empty)
     */
    public static Set<Formula> findAffectedFormulas(KB kb, Set<String> changedTerms) {

        Set<Formula> affected = new HashSet<>();
        if (changedTerms == null || changedTerms.isEmpty() || kb == null)
            return affected;

        // Determine whether any changed term is a relation; if so, formulas
        // with predicate variables may expand to include the new predicate.
        boolean predicateChanged = false;
        if (kb.kbCache != null) {
            for (String term : changedTerms) {
                if (kb.kbCache.relations.contains(term)) {
                    predicateChanged = true;
                    break;
                }
            }
        }

        // 1. Direct reference: every formula that mentions a changed term
        //    in any argument position 0-5 (predicate slot + up to 5 args).
        for (String term : changedTerms) {
            for (int argnum = 0; argnum <= 5; argnum++) {
                affected.addAll(kb.ask("arg", argnum, term));
            }
        }

        // 2. Predicate variables: if a relation changed, all formulas whose
        //    predVarCache is set and non-empty must be re-expanded because
        //    PredVarInst enumerates predicates from the (now-updated) KBcache.
        //    (predVarCache == null means the formula has not been processed yet
        //    and will be fully processed on its first translation anyway.)
        if (predicateChanged) {
            for (Formula f : kb.formulaMap.values()) {
                if (f.predVarCache != null && !f.predVarCache.isEmpty()) {
                    affected.add(f);
                }
            }
        }

        // 3. Clear varTypeCache on every affected formula.  This forces
        //    computeVariableTypes() to recompute type-guards using the updated
        //    KBcache signatures on the next preProcess() call.
        for (Formula f : affected) {
            f.varTypeCache = new HashMap<>();
        }

        return affected;
    }

    /** *************************************************************
     * Subclass-specific affected formula detection.
     *
     * <p>Replaces the generic {@link #findAffectedFormulas} for
     * {@code subclass} / {@code immediateSubclass} tells.  It avoids
     * the "scan every ancestor" explosion that the generic method
     * produces when {@code addSubclass} returns ancestors like
     * {@code Entity} and {@code Object}.
     *
     * <p>A formula is <em>affected</em> if any of the following holds:
     * <ol>
     *   <li><b>Direct mention of {@code child}</b> — formulas that
     *       reference the new (or updated) class by name.  Their type
     *       guards may be winnowed differently now that {@code child}
     *       has a new parent.</li>
     *   <li><b>Signature dependency</b> — formulas that use any relation
     *       whose {@code signatures} entry mentions {@code parent} or any
     *       of {@code parent}'s subclass ancestors.  After adding
     *       {@code (subclass child parent)}, {@code winnowTypeList()} may
     *       now resolve those variable positions to {@code child} instead
     *       of the broader type.</li>
     *   <li><b>Predicate variable expansion</b> — only if {@code child}
     *       is itself a relation in the session cache.  Such formulas are
     *       re-expanded by {@code PredVarInst} against the updated
     *       predicate set.</li>
     * </ol>
     *
     * <p>All affected formulas have their {@code varTypeCache} cleared.
     *
     * @param kb           the shared KB ({@code formulaMap} and {@code formulas} index)
     * @param sessionCache the already-updated session KBcache (not {@code kb.kbCache})
     * @param child        arg-1 of the new {@code (subclass child parent)} formula
     * @param parent       arg-2 of the new formula
     * @return formulas that require retranslation (never null, may be empty)
     */
    public static Set<Formula> findAffectedFormulasForSubclass(
            KB kb, KBcache sessionCache, String child, String parent) {

        Set<Formula> affected = new HashSet<>();
        if (kb == null || sessionCache == null) return affected;

        // 1. Direct mention of child.
        for (int i = 0; i <= 5; i++)
            affected.addAll(kb.ask("arg", i, child));

        // NOTE: Signature / type-guard dependency (path 2) is intentionally omitted.
        //
        // The theoretically correct criterion would be: find formulas that constrain the
        // same variable to BOTH child (via child's signature entry) AND parent/ancestor
        // (via another predicate in the same formula), so that winnowTypeList() now picks
        // child.  In practice this requires child to already appear in some domain/range
        // declaration AND for a formula to use two such predicates on the same variable.
        //
        // Scanning from child's signature entries causes too many false positives:
        //   - kb.ask("arg", N, rel) sweeps in ground facts (instance rel Class) that
        //     mention the relation name but have no variables and therefore no type guards.
        //   - format/documentation formulas (excluded from TPTP) produce warnings.
        //
        // Impact of omitting this path: at worst a formula retains a slightly redundant
        // type guard, e.g. (and (instance ?X Man) (instance ?X GreekAncestor)) instead of
        // (instance ?X Man).  That is a harmless over-specification — provers still find
        // the same answers.  The correctness benefit does not justify the retranslation cost.

        // 2. Predicate variable expansion — only if child is now a relation.
        if (sessionCache.relations.contains(child)) {
            for (Formula f : kb.formulaMap.values())
                if (f.predVarCache != null && !f.predVarCache.isEmpty())
                    affected.add(f);
        }

        // 4. Clear varTypeCache to force type-guard recomputation.
        for (Formula f : affected)
            f.varTypeCache = new HashMap<>();

        return affected;
    }

    /** *************************************************************
     * Instance-specific affected formula detection.
     *
     * <p>Replaces the generic {@link #findAffectedFormulas} for
     * {@code instance} / {@code immediateInstance} tells.  Adding
     * {@code (instance inst className)} does <em>not</em> alter
     * {@code signatures}, so no type-guard changes propagate to
     * unrelated formulas.
     *
     * <p>A formula is <em>affected</em> if any of the following holds:
     * <ol>
     *   <li><b>Direct mention of {@code inst}</b> — formulas that
     *       reference the instance by name.</li>
     *   <li><b>Predicate variable expansion</b> — only if {@code inst}
     *       is now a relation in the session cache, because
     *       {@code PredVarInst} enumerates the updated relations set.</li>
     * </ol>
     *
     * <p>All affected formulas have their {@code varTypeCache} cleared.
     *
     * @param kb           the shared KB ({@code formulaMap} and {@code formulas} index)
     * @param sessionCache the already-updated session KBcache (not {@code kb.kbCache})
     * @param inst         arg-1 of the new {@code (instance inst className)} formula
     * @param className    arg-2 of the new formula
     * @return formulas that require retranslation (never null, may be empty)
     */
    public static Set<Formula> findAffectedFormulasForInstance(
            KB kb, KBcache sessionCache, String inst, String className) {

        Set<Formula> affected = new HashSet<>();
        if (kb == null || sessionCache == null) return affected;

        // 1. Direct mention of inst.
        for (int i = 0; i <= 5; i++)
            affected.addAll(kb.ask("arg", i, inst));

        // 2. Predicate variable expansion — only if inst is now a relation.
        if (sessionCache.relations.contains(inst)) {
            for (Formula f : kb.formulaMap.values())
                if (f.predVarCache != null && !f.predVarCache.isEmpty())
                    affected.add(f);
        }

        // 3. Clear varTypeCache to force type-guard recomputation.
        for (Formula f : affected)
            f.varTypeCache = new HashMap<>();

        return affected;
    }

    /** *************************************************************
     * Retranslate a set of formulas using the current {@code kb.kbCache}.
     *
     * <p>The caller is responsible for swapping {@code kb.kbCache} to the desired
     * session-specific cache <em>before</em> calling this method and restoring it
     * afterwards.  Each formula's {@code theFofFormulas} / {@code theTffFormulas}
     * caches are cleared and repopulated as a side-effect.
     *
     * <p>The returned map preserves insertion order (LinkedHashMap) so that callers
     * can append the new axiom lines in a deterministic order.
     *
     * @param kb       the knowledge base (kbCache should already be the session cache)
     * @param formulas the formulas to retranslate
     * @param lang     "fof" or "tff"
     * @return map from each formula to its list of new TPTP body strings (sort decls
     *         and formula axioms); if the formula produces no output the list is empty
     */
    public static Map<Formula, List<String>> retranslateFormulas(
            KB kb, Set<Formula> formulas, String lang) {

        if (formulas == null || formulas.isEmpty())
            return Collections.emptyMap();

        SUMOKBtoTPTPKB translator = new SUMOKBtoTPTPKB();
        translator.kb = kb;
        setLang(lang);
        int total = formulas.size();

        Map<Formula, List<String>> result = new LinkedHashMap<>();
        int formulaIndex = 0;
        for (Formula f : formulas) {
            FormulaResult res = translator.translateOneFormula(f, lang, total, formulaIndex++);
            List<String> bodies = new ArrayList<>();
            if (!res.skipEverything && !res.skippedHOL && !res.skippedCached) {
                // Sort declarations (TFF-specific, typically empty for FOF)
                for (String sort : res.sortBodies) {
                    if (!StringUtil.emptyString(sort) && !bodies.contains(sort))
                        bodies.add(sort);
                }
                // TPTP body strings — apply content filters but not global dedup
                // (dedup against the patched file is handled by the caller)
                List<String> filterLog = new ArrayList<>();
                for (String tptp : res.tptpBodies) {
                    if (!StringUtil.emptyString(tptp) && !bodies.contains(tptp)
                            && !translator.filterAxiom(f, tptp, filterLog))
                        bodies.add(tptp);
                }
            }
            result.put(f, bodies);
        }
        return result;
    }

    /** *************************************************************
     */
    public String getSanitizedKBname() {

        return kb.name.replaceAll("\\W","_");
    }

    /** *************************************************************
     */
    public static String langToExtension(String l) {

        if (l.equals("fof"))
            return "tptp";
        return l;
    }

    /** *************************************************************
     */
    public static String extensionToLang(String l) {

        if (l.equals("tptp"))
            return "fof";
        return l;
    }

    /** *************************************************************
     */
    public String getInfFilename() {

        String sanitizedKBName = getSanitizedKBname();
        return KBmanager.getMgr().getPref("kbDir") + File.separator +
                sanitizedKBName + "." + langToExtension(getLang());
    }

    /** *************************************************************
     */
    public String copyFile(String fileName) {

        String outputPath = "";
        try {
            String sanitizedKBName = getSanitizedKBname();
            File inputFile = new File(fileName);
            File outputFile = File.createTempFile(sanitizedKBName, ".p", null);
            outputPath = outputFile.getCanonicalPath();
            try (Reader in = new FileReader(inputFile);
                 Writer out = new FileWriter(outputFile)) {
                int c;
                while ((c = in.read()) != -1)
                    out.write(c);
            }
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
        return outputPath;
    }

    /** *************************************************************
     */
    public static void addToFile(String fileName, List<String> axioms, String conjecture) {

        boolean append = true;
        try (OutputStream file = new FileOutputStream(fileName, append);
             DataOutputStream out = new DataOutputStream(file)) {
            if (axioms != null) {   // add axioms
                for (String axiom : axioms)
                    out.writeBytes(axiom);
                out.flush();
            }
            if (StringUtil.isNonEmptyString(conjecture)) {  // add conjecture
                out.writeBytes(conjecture);
                out.flush();
            }
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /** ***************************************************************
     * @param relationMap is a Map of String keys and values where
     *                    the key is the renamed relation and the
     *                    value is the original name.
     */
    protected void printVariableArityRelationContent(PrintWriter pr, Map<String,String> relationMap,
                                                     String sanitizedKBName, AtomicInteger axiomIndex) {

        Iterator<String> it = relationMap.keySet().iterator();
        String key, value;
        List<Formula> result;
        String s;
        while (it.hasNext()) {
            key = it.next();
            value = relationMap.get(key);
            result = kb.ask("arg",1,value);
            if (result != null) {
                for (Formula f : result) {
                    s = f.getFormula().replace(value,key);
                    String varArityTPTP = ExprToTPTP.translateKifString(s, false, getLang());
                    if (varArityTPTP == null)
                        varArityTPTP = SUMOformulaToTPTPformula.tptpParseSUOKIFString(s, false);
                    pr.println(getLang() + "(kb_" + sanitizedKBName + "_" + axiomIndex.getAndIncrement() +
                            ",axiom,(" + varArityTPTP + ")).");
                }
            }
        }
    }

    /** ***************************************************************
     * Print the sorts of any numeric constants encountered during processing.
     * They are stored in SUMOtoTFAform.getNumericConstantTypes()
     */
    public void printTFFNumericConstants(PrintWriter pw) {

        int size = SUMOtoTFAform.getNumericConstantTypes().keySet().size();
        if (size == SUMOtoTFAform.numericConstantCount)
            return;
        String type;
        List<String> sortedKeys = new ArrayList<>(SUMOtoTFAform.getNumericConstantTypes().keySet());
        Collections.sort(sortedKeys);
        for (String t : sortedKeys) {
            if (SUMOtoTFAform.numericConstantValues.keySet().contains(t))
                continue;
            type = SUMOtoTFAform.getNumericConstantTypes().get(t);
            if (debug) System.out.println("SUMOKBtoTPTPKB.printTFFNumericConstants(): term, type: " + t + ", " + type);
            if (isTptpArithmeticLiteral(t)) continue;
            pw.println("tff(" + SUMOformulaToTPTPformula.translateWord(t, StreamTokenizer.TT_WORD,false)
                    + "_sig,type," + SUMOformulaToTPTPformula.translateWord(t, StreamTokenizer.TT_WORD,false)
                    + ":" + SUMOKBtoTFAKB.translateSort(kb,type) + ").");
        }
//        for (String t : SUMOtoTFAform.getNumericConstantTypes().keySet()) {
//            if (SUMOtoTFAform.numericConstantValues.keySet().contains(t))
//                continue;
//        }
    }

    /** ***************************************************************
     * Print the sorts of any numeric constants encountered during processing.
     * They are stored in SUMOtoTFAform.getNumericConstantTypes()
     */
    public synchronized void printTFFNumericConstants(List<String> fileContents) {

        int size = SUMOtoTFAform.getNumericConstantTypes().keySet().size();
        if (size == SUMOtoTFAform.numericConstantCount)
            return;
        String type;
        List<String> sortedKeys = new ArrayList<>(SUMOtoTFAform.getNumericConstantTypes().keySet());
        Collections.sort(sortedKeys);
        for (String t : sortedKeys) {
            if (SUMOtoTFAform.numericConstantValues.keySet().contains(t))
                continue;
            type = SUMOtoTFAform.getNumericConstantTypes().get(t);
            if (debug) System.out.println("SUMOKBtoTPTPKB.printTFFNumericConstants(): term, type: " + t + ", " + type);
            if (isTptpArithmeticLiteral(t)) continue;
            fileContents.add("tff(" + SUMOformulaToTPTPformula.translateWord(t, StreamTokenizer.TT_WORD,false)
                    + "_sig,type," + SUMOformulaToTPTPformula.translateWord(t, StreamTokenizer.TT_WORD,false)
                    + ":" + SUMOKBtoTFAKB.translateSort(kb,type) + ").");
        }
    }

    /** *************************************************************
     *  Sets isQuestion and calls writeTPTPFile() below

    public String writeFile(String fileName,
                                Formula conjecture) {

        final boolean isQuestion = false;
        return writeFile(fileName,conjecture,isQuestion);
    }
*/
    /** *************************************************************
     *  Sets pw and calls writeTPTPFile() below

    public String writeFile(String fileName,
                                Formula conjecture,
                                boolean isQuestion) {

        final PrintWriter pw = null;
        return writeFile(fileName,conjecture,isQuestion,pw);
    }
*/
    /** *************************************************************
     */
    public class OrderedFormulae extends TreeSet<Formula> {

        public int compare(Object o1, Object o2) {
            Formula f1 = (Formula) o1;
            Formula f2 = (Formula) o2;
            int fileCompare = f1.sourceFile.compareTo(f2.sourceFile);
            if (fileCompare == 0) {
                fileCompare = (Integer.valueOf(f1.startLine))
                        .compareTo(f2.startLine);
                if (fileCompare == 0) {
                    fileCompare = (Long.valueOf(f1.endFilePosition))
                            .compareTo(f2.endFilePosition);
                }
            }
            return fileCompare;
        }
    }

    /** *************************************************************
     * Per-formula translation result for the parallel formula loop.
     * Translation data (expensive) is computed in parallel; dedup/write is sequential.
     */
    private static final class FormulaResult {
        boolean skipEverything;         // true for non-reasoning formulas (nothing to write)
        boolean skippedHOL;             // true when HOL formula skipped (profiling only)
        boolean skippedCached;          // true when cached formula skipped (profiling only)
        final List<String> prologueLines  = new ArrayList<>();  // % comment lines
        final List<String> numConstLines  = new ArrayList<>();  // tff numeric constant sort decls
        final List<String> sortBodies     = new ArrayList<>();  // sort body strings (dedup in sequential)
        final List<String> tptpBodies     = new ArrayList<>();  // TPTP bodies (filter+dedup in sequential)
        final Map<String, String> localRelationMap = new HashMap<>(); // variable-arity renames
        Formula formula;                // original formula (for axiomKey, filterAxiom)

        }

    /** *************************************************************
     */
    public void writeHeader(PrintWriter pw, String sanitizedKBName) {

        if (pw != null) {
            pw.println("% Articulate Software");
            pw.println("% www.ontologyportal.org www.articulatesoftware.com");
            pw.println("% This software released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.");
            pw.println("% This is a translation to TPTP of KB " + sanitizedKBName);
            pw.println("");
        }
    }

    private final AtomicInteger axiomIndex = new AtomicInteger(1); // a count appended to axiom names to make a unique ID
    private final AtomicInteger formCount = new AtomicInteger(0);
    private final AtomicInteger idxCount = new AtomicInteger(1);
    private int counter = 0;
    private long millis = 0L;

    /** *************************************************************
     *  Write all axioms in the KB to TPTP format.
     *
     * @param fileName - the full pathname of the file to write
     * @param conjecture a conjecture to query the KB with if any
     * @param isQuestion flag to denote a question being posed
     * @param pw the PrintWriter to write TPTP with
     *
     * @return the name of the KB translation to TPTP file
     */
    public String writeFile(String fileName, Formula conjecture,
                            boolean isQuestion, PrintWriter pw) {

        PredVarInst.init();
        millis = System.currentTimeMillis();
        if (!KBmanager.initialized) {
            String msg = "Error in SUMOKBtoTPTPKB.writeFile(): KB initialization not completed";
            System.err.println(msg);
            return msg;
        }

        String retVal = null;
        try {
            // (orig) sequential processing
            if (!rapidParsing)
                retVal = _writeFile(fileName, conjecture, isQuestion, pw);
            else {
                /* Experimental threading of main loop writes big SUMO in half
                 * the time as the sequential method. 2/17/25 tdn
                 */
                retVal = _tWriteFile(fileName, conjecture, isQuestion, pw);
            }

        }
        catch (Exception ex) {
            System.err.println("Error in SUMOKBtoTPTPKB.writeFile(): " + ex.getMessage());
            ex.printStackTrace();
            return retVal;
        }

        KB.axiomKey = axiomKey;
        // Skip serialization during background generation - TPTPGenerationManager handles it
//        if (!TPTPGenerationManager.isBackgroundGenerating()) {
//            System.out.println("SUMOKBtoTPTPKB.writeFile(): serializing KB after writing");
//            KBmanager.serialize();
//        }
        if (debug) System.out.println("SUMOKBtoTPTPKB.writeFile(): axiomKey: " + axiomKey.size());
        if (debug) System.out.println("SUMOKBtoTPTPKB.writeFile(): seconds: " + (System.currentTimeMillis() - millis) / KButilities.ONE_K);

        axiomIndex.set(1); // reset
        counter = 0; // reset
        formCount.set(0); // reset
        millis = 0L; // reset
        idxCount.set(1); // reset

        return retVal;
    }

    /** *************************************************************
     * Conventional/sequential version
     * @deprecated
     */
    @Deprecated(forRemoval = true)
    private String _writeFile(String fileName, Formula conjecture,
                            boolean isQuestion, PrintWriter pw) {

        Map<String,String> relationMap = new TreeMap<>(); // A Map of variable arity relations keyed by new name
        writeHeader(pw,fileName);

        OrderedFormulae orderedFormulae = new OrderedFormulae();
        orderedFormulae.addAll(kb.formulaMap.values());
        //if (debug) System.out.println("INFO in SUMOKBtoTPTPKB.writeFile(): added formulas: " + orderedFormulae.size());

        int total = orderedFormulae.size();
        FormulaPreprocessor fp = new FormulaPreprocessor();
        Set<Formula> processed, withRelnRenames;
        String result, name;
        SUMOtoTFAform stfa;
        for (Formula f : orderedFormulae) {

            // Skip Formulas that start with "documentation, termFormat, format""
            if (isNonReasoningForATP(f.getFormula())) continue;

            // Format-specific cache clearing to prevent FOF/TFF overwrites
            if (getLang().equals("fof")) {
                f.theFofFormulas.clear();
            } else if (getLang().equals("tff")) {
                f.theTffFormulas.clear();
            }
            f.theTptpFormulas.clear(); // Legacy compatibility
            if (debug) System.out.println("SUMOKBtoTPTPKB.writeFile() : source line: " + f.startLine);
            if (!f.getFormula().startsWith("(documentation")) {
                pw.println("% f: " + f.getFormula());
                if (!f.derivation.parents.isEmpty()) {
                    for (Formula derivF : f.derivation.parents)
                        pw.println("% original f: " + derivF.getFormula());
                }
                pw.println("% " + formCount.getAndIncrement() + " of " + total +
                        " from file " + f.sourceFile + " at line " + f.startLine);
            }
            boolean isHOL = isHigherOrderExpr(f, kb);
            if (isHOL) {
                pw.println("% is higher order");
                if (getLang().equals("thf")) {  // TODO create a flag for adding modals (or not)
                    f = Modals.processModals(f,kb);
                }
                if (removeHOL)
                    continue;
            }
            else
                pw.println("% not higher order");
            if (!KBmanager.getMgr().prefEquals("cache","yes") && f.isCached())
                continue;
            if (counter++ % 100 == 0) /*System.out.print(".");*/ progressSb.append(".");
            if ((counter % 4000) == 1) {
                if (debug) System.out.print(progressSb.toString() + "x");
                progressSb.setLength(0);
                if (debug) System.out.printf("%nSUMOKBtoTPTPKB.writeFile(%s) : still working. %d%% done.%n",fileName, counter*100/total);
            }
            if (debug) System.out.println("SUMOKBtoTPTPKB.writeFile() : process: " + f);
            processed = fp.preProcess(f,false,kb);
            if (debug) System.out.println("SUMOKBtoTPTPKB.writeFile() : processed: " + processed);
            if (!processed.isEmpty()) {
                withRelnRenames = new TreeSet<>(); // sorted for deterministic output
                for (Formula f2 : new TreeSet<>(processed))
                    withRelnRenames.add(f2.renameVariableArityRelations(kb,relationMap));
                for (Formula f3 : withRelnRenames) {
                    switch (getLang()) {
                        case "fof":
                            if (debug) System.out.println("SUMOKBtoTPTPKB.writeFile() : % tptp input: " + f3.format("", "", Formula.SPACE));
                            result = ExprToTPTP.translateKifString(f3.getFormula(), false, "fof");
                            if (result == null) // fallback to legacy string-based translator
                                result = SUMOformulaToTPTPformula.tptpParseSUOKIFString(f3.getFormula(), false);
                            if (debug) System.out.println("INFO in SUMOKBtoTPTPKB.writeFile(): result: " + result);
                            if (result != null) {
                                f.theFofFormulas.add(result);
                                f.theTptpFormulas.add(result); // Legacy compatibility
                            }
                            break;
                        case "tff":
                            pw.println("% tff input: " + f3.format("", "", Formula.SPACE));
                            if (debug) System.out.println("SUMOKBtoTPTPKB.writeFile() : % tff input: " + f3.format("", "", " "));
                            // Fast path: Expr-based TFF translation (no KB type walk, sort inference from formula only)
                            result = ExprToTFF.translateKifString(f3.getFormula(), false, kb);
                            if (result != null && !result.isBlank()) {
                                tffExprCount.incrementAndGet();
                                // Sort annotations are embedded in variable lists by ExprToTFF;
                                // no separate tff(...,type,...) declarations needed.
                            }
                            else {
                                // Fallback: full SUMOtoTFAform processing with KB type inference
                                tffStringCount.incrementAndGet();
                                stfa = new SUMOtoTFAform();
                                SUMOtoTFAform.kb = kb;
                                stfa.sorts = stfa.missingSorts(f3);
                                if (stfa.sorts != null && !stfa.sorts.isEmpty())
                                    f3.tffSorts.addAll(stfa.sorts);
                                result = SUMOtoTFAform.process(f3.getFormula(), false);
                                printTFFNumericConstants(pw);
                                SUMOtoTFAform.initNumericConstantTypes();
                            }
                            if (!StringUtil.emptyString(result)) {
                                f.theTffFormulas.add(result);
                                f.theTptpFormulas.add(result); // Legacy compatibility
                            }
                            else if (!StringUtil.emptyString(SUMOtoTFAform.getFilterMessage())) {
                                pw.println("% " + SUMOtoTFAform.getFilterMessage());
                            }
                            break;
                        default:
                            pw.println("% unhandled language option " + getLang());
                            break;
                    }
                }
            }
            else {
                //System.out.println("SUMOKBtoTPTPKB.writeFile() : % empty result from preprocess on " + f.getFormula().replace("\\n"," "));
                pw.println("% empty result from preprocess on " + f.getFormula().replace("\\n",Formula.SPACE));
            }
            for (String sort : new TreeSet<>(f.tffSorts)) {
                if (!StringUtil.emptyString(sort) &&
                        !alreadyWrittenTPTPs.contains(sort)) {
                    name = "kb_" + getSanitizedKBname() + "_" + axiomIndex.getAndIncrement();
                    putAxiom(name,f);
                    pw.println(getLang() + Formula.LP + name + ",type," + sort + ").");
                    alreadyWrittenTPTPs.add(sort);
                }
            }
            // Use format-specific field for file writing
            Set<String> formulasToWrite = getLang().equals("fof") ? f.theFofFormulas : f.theTffFormulas;
            for (String theTPTPFormula : new TreeSet<>(formulasToWrite)) {
                if (!StringUtil.emptyString(theTPTPFormula) &&
                        !alreadyWrittenTPTPs.contains(theTPTPFormula) &&
                        !filterAxiom(f,theTPTPFormula,pw)) {
                    if (debug) System.out.println("SUMOKBtoTPTPKB.writeFile() : writing " + theTPTPFormula);
                    name = "kb_" + getSanitizedKBname() + "_" + axiomIndex.getAndIncrement();
                    putAxiom(name,f);
                    pw.println(getLang() + Formula.LP + name + ",axiom,(" + theTPTPFormula + ")).");
                    if (debug) System.out.println("SUMOKBtoTPTPKB.writeFile() : finished writing " + theTPTPFormula + " with name " + name);
                    alreadyWrittenTPTPs.add(theTPTPFormula);
                }
//                else
//                    pw.println("% empty, already written or filtered formula, skipping : " + theTPTPFormula);
            }
        } // end outer (main) for loop
        if (debug) System.out.println();
        printVariableArityRelationContent(pw,relationMap,getSanitizedKBname(),axiomIndex);
        printTFFNumericConstants(pw);
        if (debug) System.out.println("SUMOKBtoTPTPKB.writeFile() CWA: " + CWA);
        if (CWA)
            pw.println(StringUtil.arrayListToCRLFString(CWAUNA.run(kb)));
        if (conjecture != null) {  //----Print conjecture if one has been supplied
            // conjecture.getTheTptpFormulas() should return a
            // List containing only one String, so the iteration
            // below is probably unnecessary
            String type = "conjecture";
            if (isQuestion) type = "question";
            for (String theTPTPFormula : conjecture.theTptpFormulas)
                pw.println(getLang() + "(prove_from_" + getSanitizedKBname() + "," + type + ",(" + theTPTPFormula + ")).");
        }
        pw.flush();

        relationMap.clear();
        orderedFormulae.clear();
        progressSb.setLength(0);

        return getInfFilename();
    }


    /**
     * Expr-based analogue of {@link Formula#isHigherOrder(KB)}.
     *
     * <p>Walks the already-parsed {@link Expr} tree without constructing any
     * {@link Formula} objects or calling {@code findAllTypeRestrictions}.
     * The logic mirrors {@code Formula.isHigherOrder} exactly:
     * <ul>
     *   <li>If the predicate's signature contains {@code "Formula"} → HOL.</li>
     *   <li>For logical operators: recurse into each compound (non-atom, non-function) arg.</li>
     *   <li>For regular predicates: any compound non-function arg → HOL immediately.</li>
     * </ul>
     */
    private static boolean isHigherOrderExpr(Expr expr, KB kb) {
        if (!(expr instanceof Expr.SExpr se)) return false;
        String head = se.headName();
        if (head == null) return false; // var-list node inside a quantifier
        List<String> sig = kb.kbCache.getSignature(head);
        if (sig != null && !Formula.isVariable(head) && sig.contains("Formula"))
            return true;
        boolean logop = Formula.isLogicalOperator(head);
        for (Expr arg : se.args()) {
            if (!(arg instanceof Expr.SExpr argSe)) continue; // atom/var/literal — not HOL
            String argHead = argSe.headName();
            if (argHead != null && !kb.isFunction(argHead)) {
                // compound, non-function arg
                if (logop) {
                    if (isHigherOrderExpr(argSe, kb)) return true; // recurse for logical ops
                } else {
                    return true; // compound non-function arg to non-logop predicate → HOL
                }
            } else {
                // function application (or null-head var-list) — recurse
                if (isHigherOrderExpr(argSe, kb)) return true;
            }
        }
        return false;
    }

    /**
     * Returns true if {@code expr} contains a {@link Expr.Var} in predicate
     * position (i.e., as the head of an {@link Expr.SExpr}).
     *
     * Such formulas cannot be expressed in FOF or TFF after pred-var expansion
     * has failed to find any KB instances for the variable's type constraint.
     * They should be skipped rather than emitted as malformed first-order axioms.
     */
    static boolean hasUnresolvedPredVar(Expr expr) {
        return switch (expr) {
            case Expr.SExpr se -> {
                if (se.head() instanceof Expr.Var) yield true;  // Var in predicate position
                if (se.head() != null && hasUnresolvedPredVar(se.head())) yield true;
                yield se.args().stream().anyMatch(SUMOKBtoTPTPKB::hasUnresolvedPredVar);
            }
            default -> false;
        };
    }

    /**
     * Dispatches to {@link #isHigherOrderExpr(Expr, KB)} when the formula
     * has a parsed {@link Expr} tree, otherwise falls back to
     * {@link Formula#isHigherOrder(KB)}.
     */
    private static boolean isHigherOrderExpr(Formula f, KB kb) {
        if (f instanceof FormulaAST fa && fa.expr != null) {
            boolean hol = isHigherOrderExpr(fa.expr, kb);
            if (hol) fa.higherOrder = true;
            return hol;
        }
        return f.isHigherOrder(kb);
    }

    private static boolean isNonReasoningForATP(String kif) {
        // keep it cheap: no parsing, just prefix checks / contains checks
        return kif.startsWith("(termFormat ")
                || kif.startsWith("(format ")
                || kif.startsWith("(documentation");
    }

    /** *************************************************************
     * Collect TFF numeric constant sort declarations from this thread's ThreadLocal
     * into the provided list. Called from parallel lambda — reads only the current
     * thread's TL (no shared state); avoids the synchronized printTFFNumericConstants.
     */
    private void collectNumericConstants(List<String> lines) {

        Map<String, String> ncts = SUMOtoTFAform.getNumericConstantTypes();
        if (ncts.size() == SUMOtoTFAform.numericConstantCount)
            return;
        List<Map.Entry<String, String>> sortedEntries = new ArrayList<>(ncts.entrySet());
        sortedEntries.sort(Map.Entry.comparingByKey());
        for (Map.Entry<String, String> e : sortedEntries) {
            String t = e.getKey();
            if (SUMOtoTFAform.numericConstantValues.containsKey(t)) continue;
            String type = e.getValue();
            if (isTptpArithmeticLiteral(t)) continue;
            lines.add("tff(" + SUMOformulaToTPTPformula.translateWord(t, StreamTokenizer.TT_WORD, false)
                    + "_sig,type,"
                    + SUMOformulaToTPTPformula.translateWord(t, StreamTokenizer.TT_WORD, false)
                    + ":" + SUMOKBtoTFAKB.translateSort(kb, type) + ").");
        }
    }

    /**
     * Returns true if {@code name} is a TPTP arithmetic literal (integer, rational,
     * or real) that is already typed by TFF arithmetic.  Such values cannot appear as
     * the symbol in a {@code tff(name,type,symbol:sort)} declaration, so callers should
     * skip generating a type signature for them.
     *
     * Covers:  integers  -?[0-9]+
     *          rationals -?[0-9]+/[0-9]+
     *          reals     -?[0-9]+\.[0-9]*  or  -?[0-9]*\.[0-9]+
     */
    static boolean isTptpArithmeticLiteral(String name) {
        if (name == null || name.isEmpty()) return false;
        return name.matches("-?[0-9]+(\\.[0-9]*|/[0-9]+)?|-?[0-9]*\\.[0-9]+");
    }

    /** *************************************************************
     * Translate a single formula into a FormulaResult (parallel-safe).
     * All operations here use only per-formula or ThreadLocal state; shared
     * fields (alreadyWrittenTPTPs, axiomKey, relationMap) are NOT touched.
     */
    private FormulaResult translateOneFormula(Formula formula, String localLang, int total, int formulaIndex) {

        FormulaResult res = new FormulaResult();
        res.formula = formula;
        Formula f = formula;

        // Session isolation: skip UA formulas from other sessions during base generation.
        // Base generation (sessionId==null) must never include session-specific assertions —
        // they belong only in session TPTP files.  Cross-session formulas pollute the shared
        // SUMO.tptp and corrupt any other session that reads it.
        if (f.uaSessionId != null) {
            res.skipEverything = true;
            return res;
        }

        // Non-reasoning formulas: skip entirely (nothing to write)
        if (isNonReasoningForATP(f.getFormula())) {
            res.skipEverything = true;
            return res;
        }

        // Format-specific cache clearing (safe: each formula is assigned to exactly one thread)
        if (localLang.equals("fof")) {
            f.theFofFormulas.clear();
        } else if (localLang.equals("tff")) {
            f.theTffFormulas.clear();
        }
        f.theTptpFormulas.clear(); // Legacy compatibility

        // Prologue comment lines (getFormula() is O(1) vs format() which scans char-by-char)
        res.prologueLines.add("% f: " + f.getFormula());
        if (!f.derivation.parents.isEmpty()) {
            for (Formula derivF : f.derivation.parents)
                res.prologueLines.add("% original f: " + derivF.getFormula());
        }
        res.prologueLines.add("% " + formulaIndex + " of " + total
                + " from file " + f.sourceFile + " at line " + f.startLine);

        // HOL check — use Expr fast path when available (avoids findAllTypeRestrictions +
        // Formula allocation per arg in the original string-based isHigherOrder)
        boolean isHOL = isHigherOrderExpr(f, kb);
        if (isHOL) {
            f.higherOrder = true;
            res.prologueLines.add("% is higher order");
            if (localLang.equals("thf"))
                f = Modals.processModals(f, kb);
            if (removeHOL) {
                res.skippedHOL = true;
                return res;
            }
        } else {
            res.prologueLines.add("% not higher order");
        }

        // Cache check
        if (!KBmanager.getMgr().prefEquals("cache", "yes") && f.isCached()) {
            res.skippedCached = true;
            return res;
        }

        if (debug) System.out.println("SUMOKBtoTPTPKB.writeFile() : process: " + f);

        // ---- preprocess + translate ----
        FormulaPreprocessor fp = new FormulaPreprocessor();

        // Expr fast path: FormulaAST with an Expr bypasses the string-scanning
        // preProcessRecurse() and the parseSentence() re-parse in translateKifString().
        // Pred-var instantiation (Phase A) and row-var expansion (Phase B) are now
        // handled inside preProcessExpr(FormulaAST,...); variable-arity renaming
        // and type restrictions follow in Phase C.
        boolean usedExprPath = false;
        if ((localLang.equals("fof") || localLang.equals("tff")) &&
                formula instanceof FormulaAST && ((FormulaAST) formula).expr != null) {

            FormulaAST fa = (FormulaAST) formula;
            if (loggedExprPath.compareAndSet(false, true))
                System.out.println("SUMOKBtoTPTPKB.translateOneFormula(): using EXPR path for formula=" + fa.getFormula());
            Set<Expr> processedExprs = fp.preProcessExpr(fa, false, kb);
            if (processedExprs != null && !processedExprs.isEmpty()) {
                usedExprPath = true;
                if (localLang.equals("fof")) fofExprCount.incrementAndGet();
                else                         tffExprCount.incrementAndGet();
                for (Expr pexpr : processedExprs) {
                    // A Var in predicate position means pred-var expansion found no KB instances
                    // and kept the original. Such a formula cannot be expressed in FOF/TFF — skip it.
                    if (hasUnresolvedPredVar(pexpr)) {
                        res.prologueLines.add("% skipped: unresolved pred-var (no KB instances for type constraint): "
                                + pexpr.toKifString());
                        continue;
                    }
                    if (localLang.equals("fof")) {
                        if (debug) System.out.println("SUMOKBtoTPTPKB.writeFile() : % expr path fof input: "
                                + pexpr.toKifString());
                        String fofResult = ExprToTPTP.translate(pexpr, false, "fof");
                        if (fofResult == null) { // fallback to legacy string-based translator
                            String kifStr = pexpr.toKifString();
                            fofResult = SUMOformulaToTPTPformula.tptpParseSUOKIFString(kifStr, false);
                        }
                        if (debug) System.out.println("INFO in SUMOKBtoTPTPKB.writeFile(): fof result: " + fofResult);
                        if (fofResult != null) {
                            f.theFofFormulas.add(fofResult);
                            f.theTptpFormulas.add(fofResult); // Legacy compatibility
                        }
                    } else { // tff
                        if (debug) System.out.println("SUMOKBtoTPTPKB.writeFile() : % expr path tff input: "
                                + pexpr.toKifString());
                        // Fast path: ExprToTFF translates directly from Expr tree with inline sort annotations
                        String tffResult = ExprToTFF.translate(pexpr, false, kb);
                        if (tffResult == null || tffResult.isBlank()) {
                            // Fallback: SUMOtoTFAform.processExpr() with full KB type inference
                            System.out.println("ExprToTFF.translate() fallback (expr path): " + pexpr.toKifString());
                            SUMOtoTFAform stfa = new SUMOtoTFAform();
                            SUMOtoTFAform.kb = kb;
                            stfa.sorts = stfa.missingSortsExpr(pexpr);
                            if (stfa.sorts != null && !stfa.sorts.isEmpty())
                                f.tffSorts.addAll(stfa.sorts);
                            tffResult = SUMOtoTFAform.processExpr(pexpr, false);
                            collectNumericConstants(res.numConstLines);
                            SUMOtoTFAform.getNumericConstantTypes().clear();
                            SUMOtoTFAform.getNumericConstantTypes().put("NumberE", "RealNumber");
                            SUMOtoTFAform.getNumericConstantTypes().put("Pi", "RealNumber");
                        }
                        if (!StringUtil.emptyString(tffResult)) {
                            f.theTffFormulas.add(tffResult);
                            f.theTptpFormulas.add(tffResult); // Legacy compatibility
                        }
                        else if (!StringUtil.emptyString(SUMOtoTFAform.getFilterMessage()))
                            res.prologueLines.add("% " + SUMOtoTFAform.getFilterMessage());
                    }
                }
            }
        } // end if ((fof || tff) && FormulaAST)

        // ---- string-based pre-processing ----
        if (!usedExprPath) {
            if (localLang.equals("fof")) fofStringCount.incrementAndGet();
            else                         tffStringCount.incrementAndGet();
            // Diagnose every string-path fallback for tff (only ~6 formulas — safe to log all)
            if (localLang.equals("tff")) {
                String reason;
                if (!(formula instanceof FormulaAST))
                    reason = "not FormulaAST (class=" + formula.getClass().getSimpleName() + ")";
                else if (((FormulaAST) formula).expr == null)
                    reason = "FormulaAST.expr == null";
                else
                    reason = "preProcessExpr returned empty";
                System.out.println("SUMOKBtoTPTPKB.tff-string-fallback [" + reason + "]: " + f.getFormula());
            }
            // Print the first formula that fallback to string manipulation
            if (loggedStringPath.compareAndSet(false, true))
                System.out.println("SUMOKBtoTPTPKB.translateOneFormula(): using STRING path for formula=" + f.getFormula());
            Set<Formula> processed = fp.preProcess(f, false, kb);
            if (debug) System.out.println("SUMOKBtoTPTPKB.writeFile() : processed: " + processed);
            if (processed != null && !processed.isEmpty()) {
                // ---- rename ----
                Set<Formula> withRelnRenames = new TreeSet<>();
                for (Formula f2 : new TreeSet<>(processed))
                    withRelnRenames.add(f2.renameVariableArityRelations(kb, res.localRelationMap));

                for (Formula f3 : withRelnRenames) {
                    switch (localLang) {
                        case "fof":
                            if (debug) System.out.println("SUMOKBtoTPTPKB.writeFile() : % tptp input: "
                                    + f3.format("", "", " "));
                            String fofResult = ExprToTPTP.translateKifString(f3.getFormula(), false, "fof");
                            if (fofResult == null) // fallback to legacy string-based translator
                                fofResult = SUMOformulaToTPTPformula.tptpParseSUOKIFString(f3.getFormula(), false);
                            if (debug) System.out.println("INFO in SUMOKBtoTPTPKB.writeFile(): result: " + fofResult);
                            if (fofResult != null) {
                                f.theFofFormulas.add(fofResult);
                                f.theTptpFormulas.add(fofResult); // Legacy compatibility
                            }
                            break;

                        case "tff":
                            res.prologueLines.add("% tff input: " + f3.format("", "", Formula.SPACE));
                            if (debug) System.out.println("SUMOKBtoTPTPKB.writeFile() : % tff input: "
                                    + f3.format("", "", " "));

                            // Fast path: try ExprToTFF first (no heavyweight string manipulation)
                            String tffResult = ExprToTFF.translateKifString(f3.getFormula(), false, kb);

                            if (tffResult == null || tffResult.isBlank()) {
                                // Fallback: full SUMOtoTFAform processing
                                System.out.println("ExprToTFF.translateKifString() fallback (string path): " + f3.getFormula());
                                SUMOtoTFAform stfa = new SUMOtoTFAform();
                                SUMOtoTFAform.kb = kb;

                                stfa.sorts = stfa.missingSorts(f3);

                                if (stfa.sorts != null && !stfa.sorts.isEmpty())
                                    f3.tffSorts.addAll(stfa.sorts);

                                tffResult = SUMOtoTFAform.process(f3.getFormula(), false);

                                // Collect numeric constants from this thread's TL (parallel-safe, no synchronized)
                                collectNumericConstants(res.numConstLines);
                                // Reset TL for the next formula processed by this thread
                                SUMOtoTFAform.getNumericConstantTypes().clear();
                                SUMOtoTFAform.getNumericConstantTypes().put("NumberE", "RealNumber");
                                SUMOtoTFAform.getNumericConstantTypes().put("Pi", "RealNumber");
                            }

                            if (!StringUtil.emptyString(tffResult)) {
                                f.theTffFormulas.add(tffResult);
                                f.theTptpFormulas.add(tffResult); // Legacy compatibility
                            } else if (!StringUtil.emptyString(SUMOtoTFAform.getFilterMessage()))
                                res.prologueLines.add("% " + SUMOtoTFAform.getFilterMessage());
                            break;

                        default:
                            res.prologueLines.add("% unhandled language option " + localLang);
                            break;
                    }
                }
                processed.clear();
            } else {
                res.prologueLines.add("% empty result from preprocess on "
                        + f.getFormula().replace("\\n", Formula.SPACE));
            }
        } // end if (!usedExprPath)

        // Collect sort and TPTP bodies for sequential dedup+write phase (sorted for determinism)
        for (String sort : new TreeSet<>(f.tffSorts)) {
            if (!StringUtil.emptyString(sort))
                res.sortBodies.add(sort);
        }
        Set<String> formulasToWrite = localLang.equals("fof") ? f.theFofFormulas : f.theTffFormulas;
        for (String tptp : new TreeSet<>(formulasToWrite)) {
            if (!StringUtil.emptyString(tptp))
                res.tptpBodies.add(tptp);
        }

        return res;
    }

    /** *************************************************************
     * Parallel formula translation + sequential write.
     * Phase 1: translateOneFormula() runs concurrently on all formulas (thread-safe).
     * Phase 2: dedup (alreadyWrittenTPTPs), axiom naming, file write are sequential.
     *
     * @param fileName - the full pathname of the file to write
     * @param conjecture a conjecture to query the KB with if any
     * @param isQuestion flag to denote a question being posed
     * @param pw the PrintWriter to write TPTP with
     *
     * @return the name of the KB translation to TPTP file
     */
    private String _tWriteFile(String fileName, Formula conjecture,
                               boolean isQuestion, PrintWriter pw) {

        final String localLang = getLang(); // snapshot once from ThreadLocal

        Map<String, String> relationMap = new TreeMap<>(); // variable-arity relations keyed by new name
        writeHeader(pw, fileName);

        OrderedFormulae orderedFormulae = new OrderedFormulae();
        orderedFormulae.addAll(kb.formulaMap.values());
        final int total = orderedFormulae.size();

        // Convert to List for parallel indexed processing (TreeSet iteration order preserved)
        List<Formula> formulaList = new ArrayList<>(orderedFormulae);

        // ---- Diagnostic: count how many FormulaAST entries have expr populated ----
        if (localLang.equals("fof")) {
            long withExpr = formulaList.stream()
                    .filter(f -> f instanceof FormulaAST && ((FormulaAST) f).expr != null)
                    .count();
            long totalAST = formulaList.stream().filter(f -> f instanceof FormulaAST).count();
            long totalASTtranslated = formulaList.stream().filter(f -> localLang.equals("fof") && f instanceof FormulaAST && ((FormulaAST) f).expr != null).count();

            System.out.println("SUMOKBtoTPTPKB._tWriteFile(): FormulaAST=" + totalAST
                    + "/" + formulaList.size() + "  expr!=null=" + withExpr
                    + "  totalASTtranslated=" + totalASTtranslated
                    + " (cold=" + (withExpr == totalAST) + ")");
        }

        // ---- Pre-pass: populate all variable-arity signatures sequentially ----
        // Without this, parallel threads race inside copyNewPredFromVariableArity() and
        // each thread sees a different in-flight snapshot of kb.kbCache.functions when
        // instantiatePredVars() calls new TreeSet<>(kb.kbCache.functions). This race
        // produces different numbers of formula instantiations and therefore different
        // output line counts between JVM runs.
        // The flag ensures the pre-pass runs only ONCE per KB lifecycle: the first format
        // pays the cost; subsequent formats (TFF, THF) skip it entirely
        // because all signatures are already registered and copyNewPredFromVariableArity()
        // returns immediately via its fast-path check.
        // This mirrors the warm-up pattern used by THFnew.transModalTHF() / transPlainTHF().
        if (localLang.equals("tff") && !kb.kbCache.variableArityPrePopulated) {
            FormulaPreprocessor prePassFp = new FormulaPreprocessor();
            for (Formula prePassFormula : formulaList) {
                if (prePassFormula instanceof FormulaAST fa && fa.expr != null)
                    prePassFp.preProcessExpr(fa, false, kb);
                else
                    prePassFp.preProcess(prePassFormula, false, kb);
            }
            kb.kbCache.variableArityPrePopulated = true;
        }

        // ---- Phase 1: Parallel translation ----
        // Use a bounded ForkJoinPool to avoid over-subscribing CPU with THF parallel threads.
        int nProcs = Runtime.getRuntime().availableProcessors();
        ForkJoinPool pool = new ForkJoinPool(nProcs);
        List<FormulaResult> results;
        try {
            results = pool.submit(() ->
                    IntStream.range(0, formulaList.size())
                            .parallel()
                            .mapToObj(i -> translateOneFormula(formulaList.get(i), localLang, total, i))
                            .collect(Collectors.toList())
            ).get();
        } catch (Exception ex) {
            System.err.println("SUMOKBtoTPTPKB._tWriteFile(): parallel translation failed, falling back to sequential: "
                    + ex.getMessage());
            ex.printStackTrace();
            results = IntStream.range(0, formulaList.size())
                    .mapToObj(i -> translateOneFormula(formulaList.get(i), localLang, total, i))
                    .collect(Collectors.toList());
        } finally {
            pool.shutdown();
        }

        // ---- Phase 2: Sequential write (dedup, axiom naming, file I/O) ----
        String name;
        for (FormulaResult res : results) {
            if (res.skipEverything) continue;

            // Merge per-formula relation map into the global one
            relationMap.putAll(res.localRelationMap);

            List<String> linesBuf = new ArrayList<>(res.prologueLines);
            linesBuf.addAll(res.numConstLines);

            // Sort axioms (dedup + write)
            for (String sort : res.sortBodies) {
                if (!alreadyWrittenTPTPs.contains(sort)) {
                    name = "kb_" + getSanitizedKBname() + "_" + axiomIndex.getAndIncrement();
                    putAxiom(name, res.formula);
                    linesBuf.add(localLang + Formula.LP + name + ",type," + sort + ").");
                    alreadyWrittenTPTPs.add(sort);
                }
            }

            // TPTP formula axioms (filter + dedup + write)
            for (String tptp : res.tptpBodies) {
                if (!StringUtil.emptyString(tptp) && !alreadyWrittenTPTPs.contains(tptp)) {
                    boolean filtered = filterAxiom(res.formula, tptp, linesBuf);
                    if (!filtered) {
                        name = "kb_" + getSanitizedKBname() + "_" + axiomIndex.getAndIncrement();
                        putAxiom(name, res.formula);
                        linesBuf.add(localLang + Formula.LP + name + ",axiom,(" + tptp + ")).");
                        alreadyWrittenTPTPs.add(tptp);
                    }
                } else {
                    linesBuf.add("% empty, already written or filtered formula, skipping : " + tptp);
                }
            }

            // Write all lines for this formula to the output file
            for (String line : linesBuf)
                pw.println(line);
        }

        System.out.println();
        printVariableArityRelationContent(pw, relationMap, getSanitizedKBname(), axiomIndex);
        printTFFNumericConstants(pw);
        System.out.println("SUMOKBtoTPTPKB.writeFile() CWA: " + CWA);
        if (CWA)
            pw.println(StringUtil.arrayListToCRLFString(CWAUNA.run(kb)));

        if (conjecture != null) {
            String type = isQuestion ? "question" : "conjecture";
            for (String theTPTPFormula : conjecture.theTptpFormulas)
                pw.println(localLang + "(prove_from_" + getSanitizedKBname() + "," + type + ",(" + theTPTPFormula + ")).");
        }

        pw.flush();

        relationMap.clear();
        orderedFormulae.clear();
        progressSb.setLength(0);

        return getInfFilename();
    }


    /** *************************************************************
     * @return true if the given formula is simple clause,
     *   and contains one of the excluded predicates;
     * otherwise return false;
     */
    public boolean filterExcludePredicates(Formula formula) {

        boolean pass = false;
        if (formula.isSimpleClause(kb))
            pass = excludedPredicates.contains(formula.getArgument(0).toString());
        return pass;
    }

    /** *************************************************************
     * @deprecated
     */
    @Deprecated
    public boolean filterAxiom(Formula form, String tptp, PrintWriter pw) {

        //----Don't output ""ed ''ed and numbers
        if (QUOTED_CALL_PATTERN.matcher(tptp).matches() &&
                this.getClass().equals(SUMOKBtoTPTPKB.class)) { // only filter numbers in TPTP, not TFF
            pw.println("% number: " + tptp);
            return removeNum;
        }
        if (removeStrings && (tptp.contains("'") || tptp.indexOf('"') >= 0)) {
            pw.println("% f: " + form.getFormula());
            pw.println("% quoted thing");
            return true;
        }

        if (isHigherOrderExpr(form, kb))
            if (removeHOL)
                return true;
        if (!filterExcludePredicates(form)) {
            if (!alreadyWrittenTPTPs.contains(tptp)) {
                //pw.println("% not already written: " + tptp);
                return false;
            }
            else {
                pw.println("% already written: " + tptp);
                return true;
            }
        }
        else {
            pw.println("% filtered predicate: " + form.getArgument(0));
            return true;
        }
    }

    public boolean filterAxiom(Formula form, String tptp, List<String> fileContents) {

        //----Don't output ""ed ''ed and numbers
        if (QUOTED_CALL_PATTERN.matcher(tptp).matches() &&
                this.getClass().equals(SUMOKBtoTPTPKB.class)) { // only filter numbers in TPTP, not TFF
            fileContents.add("% number: " + tptp);
            return removeNum;
        }
        if (removeStrings && (tptp.contains("'") || tptp.indexOf('"') >= 0)) {
            fileContents.add("% f: " + form.getFormula());
            fileContents.add("% quoted thing");
            return true;
        }

        if (isHigherOrderExpr(form, kb))
            if (removeHOL)
                return true;
        if (!filterExcludePredicates(form)) {
            if (!alreadyWrittenTPTPs.contains(tptp)) {
                return false;
            }
            else {
                fileContents.add("% already written: " + tptp);
                return true;
            }
        }
        else {
            fileContents.add("% filtered predicate: " + form.getArgument(0));
            return true;
        }
    }

    /** *************************************************************
     * Will first write out SUMO.tptp if the KB had not yet been
     * serialized, or serialized files are older than the sources,
     * then, will write out a fresh SUMO.fof.
     *
     * @param args any given command line arguments (not currently used)
     */
    public static void main(String[] args) {

        System.out.println("SUMOKBtoTPTPKB.main(): SUMOKBtoTPTPKB.rapidParsing==" + SUMOKBtoTPTPKB.rapidParsing);
        KBmanager.getMgr().initializeOnce();
        SUMOKBtoTPTPKB skbtptpkb = new SUMOKBtoTPTPKB();
        String kbName = KBmanager.getMgr().getPref("sumokbname");
        skbtptpkb.kb = KBmanager.getMgr().getKB(kbName);
        String filename = KBmanager.getMgr().getPref("kbDir") + File.separator + kbName + "." + SUMOKBtoTPTPKB.getLang();
        String fileWritten = null;
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(Paths.get(filename)))) {
            fileWritten = skbtptpkb.writeFile(filename, null, false, pw);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        if (StringUtil.isNonEmptyString(fileWritten))
            System.out.println("File written: " + filename);
        else
            System.err.println("Could not write: " + filename);
    }
}
