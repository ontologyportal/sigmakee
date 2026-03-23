package com.articulate.sigma.parsing;

import com.articulate.sigma.IntegrationTestBase;
import com.articulate.sigma.KIF;
import com.articulate.sigma.KIFAST;
import com.articulate.sigma.SigmaTestBase;
import com.articulate.sigma.utils.StringUtil;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * Integration equivalence tests: parse the same .kif file with both {@link KIF} (legacy
 * StreamTokenizer-based parser) and {@link KIFAST} (ANTLR-based parser) and assert that
 * all output maps represent the same content.
 *
 * <h3>Known parser differences (documented here, handled by normalization below)</h3>
 * <ul>
 *   <li><b>String whitespace</b>: KIF's {@code StringUtil.normalizeSpaceChars()} collapses
 *       all whitespace sequences (including newlines inside quoted strings) to single spaces.
 *       KIFAST/ANTLR preserves the original whitespace.  The tests normalize both sides
 *       before comparing.</li>
 *   <li><b>Numeric literals as terms</b>: KIF's StreamTokenizer classifies numbers as
 *       {@code TT_WORD} tokens and adds them to {@code terms}.  KIFAST correctly wraps them
 *       in {@code Expr.NumLiteral} and excludes them.  Numbers are stripped from KIF's
 *       {@code terms} set before comparison.</li>
 * </ul>
 */
public class KIFEquivalenceTest extends IntegrationTestBase {

    private static KIF    kif;
    private static KIFAST kifast;

    @BeforeClass
    public static void parseEquivalenceFiles() throws Exception {
        // IntegrationTestBase.setup() runs first (JUnit 4 parent @BeforeClass before child).
        // KBmanager is already initialized at this point.
        String mergeKif = SigmaTestBase.KB_PATH + "/Merge.kif";
        kif    = new KIF();
        kif.readFile(mergeKif);
        kifast = new KIFAST();
        kifast.readFile(mergeKif);
    }

    // -----------------------------------------------------------------------
    // formulaMap equivalence
    // -----------------------------------------------------------------------

    @Test
    public void formulaMapSizesMatch() {
        assertEquals("formulaMap sizes must match", kif.formulaMap.size(), kifast.formulaMap.size());
    }

    @Test
    public void formulaMapKeySetsMatch() {
        // Normalize whitespace (collapse sequences to single space) so that multi-line
        // quoted strings compare equal between the two parsers.
        Set<String> kifKeys    = normalizeKeys(kif.formulaMap.keySet());
        Set<String> kifastKeys = normalizeKeys(kifast.formulaMap.keySet());

        Set<String> onlyInKif = new TreeSet<>(kifKeys);
        onlyInKif.removeAll(kifastKeys);

        Set<String> onlyInKifast = new TreeSet<>(kifastKeys);
        onlyInKifast.removeAll(kifKeys);

        assertTrue("Formulas present in KIF but missing from KIFAST (first 10): " + head(onlyInKif),
                onlyInKif.isEmpty());
        assertTrue("Formulas present in KIFAST but missing from KIF (first 10): " + head(onlyInKifast),
                onlyInKifast.isEmpty());
    }

    // -----------------------------------------------------------------------
    // terms equivalence
    // -----------------------------------------------------------------------

    @Test
    public void termSetsMatch() {
        // KIF incorrectly adds numeric literals (e.g. "-1", "12") to terms because
        // StreamTokenizer returns them as TT_WORD tokens.  KIFAST correctly excludes
        // them as NumLiteral nodes.  Strip numeric entries from KIF's set before comparing.
        Set<String> kifTerms = kif.terms.stream()
                .filter(t -> !isNumericLiteral(t))
                .collect(Collectors.toCollection(TreeSet::new));

        Set<String> onlyInKif = new TreeSet<>(kifTerms);
        onlyInKif.removeAll(kifast.terms);

        Set<String> onlyInKifast = new TreeSet<>(kifast.terms);
        onlyInKifast.removeAll(kifTerms);

        assertTrue("Terms in KIF but not KIFAST (first 10): " + head(onlyInKif),    onlyInKif.isEmpty());
        assertTrue("Terms in KIFAST but not KIF (first 10): " + head(onlyInKifast), onlyInKifast.isEmpty());
    }

    // -----------------------------------------------------------------------
    // predicate-position index (formulas map) equivalence
    // -----------------------------------------------------------------------

    @Test
    public void predicateIndexKeySetsMatch() {
        // arg-/ant-/cons-/stmt- keys contain term names (no quoted strings), so no
        // normalization is needed for the keys themselves.  The formula-string keys and
        // createID keys are also plain tokens.
        //
        // KIF incorrectly indexes numeric literals as predicate-position keys (e.g. "cons--1")
        // because its StreamTokenizer returns numbers as TT_WORD.  KIFAST correctly omits
        // them.  Strip any KIF key whose term suffix is a numeric literal before comparing.
        Set<String> kifKeys    = normalizeKeys(kif.formulas.keySet());
        Set<String> kifastKeys = normalizeKeys(kifast.formulas.keySet());

        kifKeys    = stripNumericTermKeys(kifKeys);
        kifastKeys = stripNumericTermKeys(kifastKeys);

        Set<String> onlyInKif = new TreeSet<>(kifKeys);
        onlyInKif.removeAll(kifastKeys);

        Set<String> onlyInKifast = new TreeSet<>(kifastKeys);
        onlyInKifast.removeAll(kifKeys);

        assertTrue("formulas index keys in KIF but not KIFAST (first 10): " + head(onlyInKif),
                onlyInKif.isEmpty());
        assertTrue("formulas index keys in KIFAST but not KIF (first 10): " + head(onlyInKifast),
                onlyInKifast.isEmpty());
    }

    @Test
    public void predicateIndexValueSetsMatch() {
        // Build normalized key → normalized-value-set maps for both parsers.
        Map<String, Set<String>> kifNorm    = normalizeFormulasMap(kif.formulas);
        Map<String, Set<String>> kifastNorm = normalizeFormulasMap(kifast.formulas);

        Set<String> shared = new LinkedHashSet<>(kifNorm.keySet());
        shared.retainAll(kifastNorm.keySet());

        List<String> mismatches = new ArrayList<>();
        for (String key : shared) {
            Set<String> kifVals    = kifNorm.get(key);
            Set<String> kifastVals = kifastNorm.get(key);
            if (!kifVals.equals(kifastVals) && mismatches.size() < 10)
                mismatches.add("key=" + key + "\n  kif=" + kifVals + "\n  kifast=" + kifastVals);
        }
        assertTrue("formulas index value-set mismatches (first 10):\n" + String.join("\n", mismatches),
                mismatches.isEmpty());
    }

    // -----------------------------------------------------------------------
    // Expr round-trip (KIFAST-internal consistency)
    // -----------------------------------------------------------------------

    @Test
    public void exprRoundTripForAllFormulas() {
        List<String> failures = new ArrayList<>();
        for (Map.Entry<String, FormulaAST> e : kifast.formulaMap.entrySet()) {
            FormulaAST f = e.getValue();
            assertNotNull("expr must not be null for: " + e.getKey(), f.expr);
            String formulaStr = e.getKey();
            String roundTrip  = f.expr.toKifString();
            if (!formulaStr.equals(roundTrip) && failures.size() < 10)
                failures.add("\n  formula: " + formulaStr.substring(0, Math.min(120, formulaStr.length()))
                           + "\n  expr:    " + roundTrip.substring(0, Math.min(120, roundTrip.length())));
        }
        assertTrue("Expr round-trip failures (first 10): " + failures, failures.isEmpty());
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Normalize a set of formula strings: collapse whitespace sequences to single space. */
    private static Set<String> normalizeKeys(Set<String> keys) {
        Set<String> result = new LinkedHashSet<>(keys.size());
        for (String k : keys)
            result.add(StringUtil.normalizeSpaceChars(k));
        return result;
    }

    /** Build a map with both keys and values normalized. */
    private static Map<String, Set<String>> normalizeFormulasMap(Map<String, List<String>> map) {
        Map<String, Set<String>> result = new LinkedHashMap<>(map.size());
        for (Map.Entry<String, List<String>> e : map.entrySet()) {
            String normKey = StringUtil.normalizeSpaceChars(e.getKey());
            Set<String> normVals = e.getValue().stream()
                    .map(StringUtil::normalizeSpaceChars)
                    .collect(Collectors.toCollection(TreeSet::new));
            result.merge(normKey, normVals, (a, b) -> { a.addAll(b); return a; });
        }
        return result;
    }

    /**
     * Remove predicate-position keys whose term suffix is a numeric literal.
     * E.g. {@code "cons--1"}, {@code "arg-2-3.14"} → removed.
     * These keys come from KIF's StreamTokenizer treating numbers as word tokens.
     */
    private static Set<String> stripNumericTermKeys(Set<String> keys) {
        Set<String> result = new LinkedHashSet<>();
        for (String key : keys) {
            // Predicate-position keys have the form  prefix-term  where prefix ends with a digit
            // that is the argumentNum, or a letter (ant/cons/stmt).  The term is everything after
            // the last occurrence of the prefix separator.  For "arg-2-3.14" the term is "3.14";
            // for "cons--1" the term is "-1"; for "arg-0-instance" the term is "instance".
            String term = extractTermSuffix(key);
            if (term == null || !isNumericLiteral(term))
                result.add(key);
        }
        return result;
    }

    /**
     * Extracts the term part from a predicate-position key such as {@code "arg-2-foo"},
     * {@code "cons-bar"}, or {@code "stmt-baz"}.  Returns {@code null} for keys that
     * do not follow the predicate-position key pattern (e.g. formula-string keys).
     *
     * <p>Key formats:
     * <ul>
     *   <li>{@code arg-N-term} — strip {@code "arg-N-"} prefix (N = one or more digits)</li>
     *   <li>{@code ant-term}, {@code cons-term}, {@code stmt-term} — strip fixed prefix</li>
     * </ul>
     */
    private static String extractTermSuffix(String key) {
        if (key.startsWith("arg-")) {
            // "arg-N-term": skip "arg-", then skip digits, then skip '-'
            int i = 4;
            while (i < key.length() && Character.isDigit(key.charAt(i))) i++;
            if (i < key.length() && key.charAt(i) == '-') return key.substring(i + 1);
        } else if (key.startsWith("ant-"))  return key.substring(4);
        else if (key.startsWith("cons-")) return key.substring(5);
        else if (key.startsWith("stmt-")) return key.substring(5);
        return null;
    }

    /** Returns true if {@code t} looks like a numeric literal (integer or decimal, possibly negative). */
    private static boolean isNumericLiteral(String t) {
        if (t == null || t.isEmpty()) return false;
        try {
            Double.parseDouble(t);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static <T> List<T> head(Set<T> set) {
        List<T> list = new ArrayList<>(set);
        return list.subList(0, Math.min(10, list.size()));
    }
}
