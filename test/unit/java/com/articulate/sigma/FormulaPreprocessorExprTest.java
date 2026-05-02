package com.articulate.sigma;

import com.articulate.sigma.parsing.Expr;
import com.articulate.sigma.parsing.SuokifVisitor;
import com.articulate.sigma.parsing.FormulaAST;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Unit tests for the pure-Expr type-restriction methods added to
 * {@link FormulaPreprocessor}:
 * <ul>
 *   <li>{@code findTypeRestrictionsExpr(Expr, KB)}</li>
 *   <li>{@code addTypeRestrictionsExpr(Expr, Map, KB)}</li>
 * </ul>
 *
 * These tests use a minimal hand-crafted KB (no SUMO files required).
 */
public class FormulaPreprocessorExprTest {

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Build a minimal KB whose {@code kbCache.signatures} map is populated
     * directly.  {@code sigEntries} must be in the form
     * {@code predName, arg0, arg1, ...} where arg0 is the return type (empty
     * string for non-functions) and arg1.. are argument types.
     * Append {@code "+"} to a type string to denote a domainSubclass entry.
     */
    private static KB buildKBWithSignatures(String... sigEntries) {
        KB kb = new KB("TestExprKB");
        kb.kbCache = new KBcache(kb);
        KBmanager.getMgr().setPref("cacheDisjoint", "false");
        // Parse sigEntries: "predName arg0 arg1 ..."
        for (String entry : sigEntries) {
            String[] parts = entry.split(" ");
            String pred = parts[0];
            List<String> sig = new ArrayList<>(Arrays.asList(parts).subList(1, parts.length));
            kb.kbCache.signatures.put(pred, sig);
        }
        return kb;
    }

    /** Parse a KIF string into an {@link Expr}. */
    private static Expr parseExpr(String kif) {
        SuokifVisitor visitor = SuokifVisitor.parseSentence(kif);
        assertFalse("Parse must succeed for: " + kif, visitor.result.isEmpty());
        FormulaAST ast = visitor.result.get(0);
        assertNotNull("AST must not be null for: " + kif, ast);
        assertNotNull("ast.expr must not be null for: " + kif, ast.expr);
        return ast.expr;
    }

    // ------------------------------------------------------------------
    // findTypeRestrictionsExpr
    // ------------------------------------------------------------------

    @Test
    public void testFindTypeRestrictions_singleFreeVar() {
        // (foo ?X) — foo has domain 1 = Animal
        KB kb = buildKBWithSignatures("foo  Animal");
        Expr expr = parseExpr("(foo ?X)");
        FormulaPreprocessor fp = new FormulaPreprocessor();

        Map<String, Set<String>> varmap = fp.findTypeRestrictionsExpr(expr, kb);

        assertTrue("varmap must contain ?X", varmap.containsKey("?X"));
        assertTrue("type for ?X must be Animal", varmap.get("?X").contains("Animal"));
    }

    @Test
    public void testFindTypeRestrictions_noVariables() {
        KB kb = buildKBWithSignatures("foo  Animal");
        Expr expr = parseExpr("(foo SomeConstant)");
        FormulaPreprocessor fp = new FormulaPreprocessor();

        Map<String, Set<String>> varmap = fp.findTypeRestrictionsExpr(expr, kb);

        assertTrue("varmap must be empty when no variables present", varmap.isEmpty());
    }

    // ------------------------------------------------------------------
    // addTypeRestrictionsExpr — free variable tests
    // ------------------------------------------------------------------

    /**
     * Free variable ?X with instance type restriction.
     * {@code (foo ?X)} → {@code (=> (instance ?X Animal) (foo ?X))}
     */
    @Test
    public void testFreeVar_instanceRestriction() {
        KB kb = buildKBWithSignatures("foo  Animal");
        Expr expr = parseExpr("(foo ?X)");
        FormulaPreprocessor fp = new FormulaPreprocessor();

        Map<String, Set<String>> varmap = fp.findTypeRestrictionsExpr(expr, kb);
        Expr result = fp.addTypeRestrictionsExpr(expr, varmap, kb);

        String kif = result.toKifString();
        assertTrue("must contain (instance ?X Animal): " + kif,
                kif.contains("(instance ?X Animal)"));
        assertTrue("must wrap with =>: " + kif, kif.startsWith("(=>"));
        assertTrue("must contain original clause: " + kif, kif.contains("(foo ?X)"));
    }

    /**
     * Free variable ?X with subclass (type+) restriction.
     * Signatures use the "+" marker for domainSubclass entries.
     * {@code (baz ?X)} → {@code (=> (subclass ?X Organism) (baz ?X))}
     */
    @Test
    public void testFreeVar_subclassRestriction() {
        KB kb = buildKBWithSignatures("baz  Organism+");
        Expr expr = parseExpr("(baz ?X)");
        FormulaPreprocessor fp = new FormulaPreprocessor();

        Map<String, Set<String>> varmap = fp.findTypeRestrictionsExpr(expr, kb);
        Expr result = fp.addTypeRestrictionsExpr(expr, varmap, kb);

        String kif = result.toKifString();
        assertTrue("must contain (subclass ?X Organism): " + kif,
                kif.contains("(subclass ?X Organism)"));
        assertTrue("must wrap with =>: " + kif, kif.startsWith("(=>"));
    }

    /**
     * Variable typed Entity must NOT produce a type guard (Entity is the root
     * class and restricting to it is vacuous / skipped by convention).
     */
    @Test
    public void testFreeVar_entityTypeSkipped() {
        KB kb = buildKBWithSignatures("ent  Entity");
        Expr expr = parseExpr("(ent ?X)");
        FormulaPreprocessor fp = new FormulaPreprocessor();

        Map<String, Set<String>> varmap = fp.findTypeRestrictionsExpr(expr, kb);
        Expr result = fp.addTypeRestrictionsExpr(expr, varmap, kb);

        // Result must be the original expr (reference-equal) — no guard added
        assertSame("Entity type must be skipped — result must be the original Expr",
                expr, result);
    }

    /**
     * Formula with no variables → no type restrictions → same Expr reference
     * returned.
     */
    @Test
    public void testNoVariables_unchanged() {
        KB kb = buildKBWithSignatures("foo  Animal");
        Expr expr = parseExpr("(foo SomeConstant)");
        FormulaPreprocessor fp = new FormulaPreprocessor();

        Map<String, Set<String>> varmap = fp.findTypeRestrictionsExpr(expr, kb);
        Expr result = fp.addTypeRestrictionsExpr(expr, varmap, kb);

        assertSame("formula with no variables must return the original Expr reference",
                expr, result);
    }

    // ------------------------------------------------------------------
    // addTypeRestrictionsExpr — forall quantifier
    // ------------------------------------------------------------------

    /**
     * {@code forall} quantified variable ?X gets a type guard in the antecedent.
     * {@code (forall (?X) (foo ?X))} →
     * {@code (forall (?X) (=> (instance ?X Animal) (foo ?X)))}
     */
    @Test
    public void testForall_typeGuardInAntecedent() {
        KB kb = buildKBWithSignatures("foo  Animal");
        Expr expr = parseExpr("(forall (?X) (foo ?X))");
        FormulaPreprocessor fp = new FormulaPreprocessor();

        Map<String, Set<String>> varmap = fp.findTypeRestrictionsExpr(expr, kb);
        Expr result = fp.addTypeRestrictionsExpr(expr, varmap, kb);

        String kif = result.toKifString();
        assertTrue("must start with (forall: " + kif, kif.startsWith("(forall"));
        assertTrue("must contain (instance ?X Animal): " + kif,
                kif.contains("(instance ?X Animal)"));
        // The guard must appear as an antecedent of => inside the forall body
        assertTrue("must contain (=> (instance ...) (foo ?X)): " + kif,
                kif.contains("(=> (instance ?X Animal) (foo ?X))"));
    }

    /**
     * A single guard for a {@code forall} body must NOT be wrapped in a
     * unary {@code (and ...)} — it should be used directly as the antecedent.
     * Checks the {@link FormulaPreprocessor#wrapAndExpr} single-element case.
     */
    @Test
    public void testForall_singleGuard_noUnitaryAnd() {
        KB kb = buildKBWithSignatures("foo  Animal");
        Expr expr = parseExpr("(forall (?X) (foo ?X))");
        FormulaPreprocessor fp = new FormulaPreprocessor();

        Map<String, Set<String>> varmap = fp.findTypeRestrictionsExpr(expr, kb);
        Expr result = fp.addTypeRestrictionsExpr(expr, varmap, kb);

        String kif = result.toKifString();
        // Unitary (and (instance ?X Animal)) must NOT appear
        assertFalse("unitary (and ...) must NOT appear for a single guard: " + kif,
                kif.contains("(and (instance ?X Animal))"));
    }

    // ------------------------------------------------------------------
    // addTypeRestrictionsExpr — exists quantifier
    // ------------------------------------------------------------------

    /**
     * {@code exists} quantified variable ?X gets a type guard in the body.
     * {@code (exists (?X) (foo ?X))} →
     * {@code (exists (?X) (and (instance ?X Animal) (foo ?X)))}
     */
    @Test
    public void testExists_typeGuardInBody() {
        KB kb = buildKBWithSignatures("foo  Animal");
        Expr expr = parseExpr("(exists (?X) (foo ?X))");
        FormulaPreprocessor fp = new FormulaPreprocessor();

        Map<String, Set<String>> varmap = fp.findTypeRestrictionsExpr(expr, kb);
        Expr result = fp.addTypeRestrictionsExpr(expr, varmap, kb);

        String kif = result.toKifString();
        assertTrue("must start with (exists: " + kif, kif.startsWith("(exists"));
        // Guard must be conjoined with the body inside the exists
        assertTrue("must contain (and (instance ?X Animal) (foo ?X)): " + kif,
                kif.contains("(and (instance ?X Animal) (foo ?X))"));
    }

    // ------------------------------------------------------------------
    // preProcessExpr — integration through the public API
    // ------------------------------------------------------------------

    /**
     * Verify that {@link FormulaPreprocessor#preProcessExpr} uses the new
     * Expr path and produces a type-restricted result when typePrefix=yes.
     */
    @Test
    public void testPreProcessExpr_typePrefix_yes() {
        KB kb = buildKBWithSignatures("foo  Animal");
        KBmanager.getMgr().setPref("typePrefix", "yes");
        Expr expr = parseExpr("(foo ?X)");
        FormulaPreprocessor fp = new FormulaPreprocessor();

        Set<Expr> results = fp.preProcessExpr(expr, false, kb);

        assertEquals("must return exactly one result", 1, results.size());
        Expr result = results.iterator().next();
        String kif = result.toKifString();
        assertTrue("result must contain type restriction: " + kif,
                kif.contains("(instance ?X Animal)"));
    }

    /**
     * Verify that {@link FormulaPreprocessor#preProcessExpr} returns the
     * original (renamed) Expr unchanged when typePrefix=no.
     */
    @Test
    public void testPreProcessExpr_typePrefix_no() {
        KB kb = buildKBWithSignatures("foo  Animal");
        KBmanager.getMgr().setPref("typePrefix", "no");
        Expr expr = parseExpr("(foo ?X)");
        FormulaPreprocessor fp = new FormulaPreprocessor();

        Set<Expr> results = fp.preProcessExpr(expr, false, kb);

        assertEquals("must return exactly one result", 1, results.size());
        Expr result = results.iterator().next();
        // No type restriction should be present
        assertFalse("result must NOT contain type restriction when typePrefix=no: " + result.toKifString(),
                result.toKifString().contains("instance"));
    }

    /**
     * Verify that query mode (isQuery=true) skips type restrictions even
     * when typePrefix=yes.
     */
    @Test
    public void testPreProcessExpr_isQuery_skipsTypePrefix() {
        KB kb = buildKBWithSignatures("foo  Animal");
        KBmanager.getMgr().setPref("typePrefix", "yes");
        Expr expr = parseExpr("(foo ?X)");
        FormulaPreprocessor fp = new FormulaPreprocessor();

        Set<Expr> results = fp.preProcessExpr(expr, true /* isQuery */, kb);

        assertEquals("must return exactly one result", 1, results.size());
        Expr result = results.iterator().next();
        assertFalse("query mode must NOT add type restrictions: " + result.toKifString(),
                result.toKifString().contains("instance"));
    }
}
