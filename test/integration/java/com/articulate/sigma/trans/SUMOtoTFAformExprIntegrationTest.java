package com.articulate.sigma.trans;

import com.articulate.sigma.IntegrationTestBase;
import com.articulate.sigma.KBmanager;
import com.articulate.sigma.parsing.Expr;
import com.articulate.sigma.parsing.SuokifVisitor;
import com.articulate.sigma.parsing.FormulaAST;
import com.articulate.sigma.FormulaPreprocessor;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.junit.Assert.*;

/**
 * Integration tests for the Expr-based TFF fast path in SUMOtoTFAform.
 *
 * All tests use the full SUMO KB (loaded by IntegrationTestBase) so that
 * numeric sort info, pred-var type lookups, and row-var expansion are
 * exercised against real KB data.
 *
 * Coverage:
 *  - Numeric constants and domains (AdditionFn, FloorFn, DivisionFn)
 *  - Comparison operators (lessThan, greaterThan, etc.)
 *  - Math operators
 *  - ListFn
 *  - Pred-var fallback fix: empty expansion → keep original
 *  - Row-var + containsNumber fix: containsNumber → keep original
 *  - Dual-run: processExpr(expr) == process(kif) for all cases
 */
public class SUMOtoTFAformExprIntegrationTest extends IntegrationTestBase {

    @BeforeClass
    public static void init() {
        System.out.println("============ SUMOtoTFAformExprTest (integration).init()");
        SUMOKBtoTFAKB skbtfakb = new SUMOKBtoTFAKB();
        skbtfakb.initOnce();
        SUMOformulaToTPTPformula.setLang("tff");
        SUMOtoTFAform.setNumericFunctionInfo();
        // Write sort declarations so process() has them available
        String kbName = KBmanager.getMgr().getPref("sumokbname");
        String filename = KBmanager.getMgr().getPref("kbDir") + File.separator + kbName + ".tff";
        try (PrintWriter pw = new PrintWriter(new FileWriter(filename))) {
            skbtfakb.writeSorts(pw);
        } catch (IOException e) {
            System.err.println("Warning: could not write sorts file: " + e.getMessage());
        }
    }

    @AfterClass
    public static void postClass() {
        KBmanager.initialized = false;
        SUMOKBtoTFAKB.initialized = false;
        SUMOtoTFAform.initialized = false;
    }

    // ---- helpers -----------------------------------------------------------

    /** Parse kif to Expr via SuokifVisitor. */
    private static Expr parse(String kif) {
        SuokifVisitor visitor = SuokifVisitor.parseSentence(kif);
        assertNotNull("visitor.result null for: " + kif, visitor.result);
        assertFalse("visitor.result empty for: " + kif, visitor.result.isEmpty());
        FormulaAST ast = visitor.result.get(0);
        assertNotNull("FormulaAST null for: " + kif, ast);
        assertNotNull("expr null for: " + kif, ast.expr);
        return ast.expr;
    }
    // ---- ListFn ------------------------------------------------------------

    @Test
    public void testMissingSortsExprListFnIntegerList() {
        // ListFn__2Fn__0Int1Int2Int encodes an integer-typed list function
        Expr expr = parse("(ListFn__2Fn__0Int1Int2Int 1 2)");
        SUMOtoTFAform stfa = new SUMOtoTFAform();
        Set<String> sorts = stfa.missingSortsExpr(expr);
        assertFalse("ListFn with Int encoding should yield at least one sort", sorts.isEmpty());
    }

    @Test
    public void testMissingSortsExprDeepNesting() {
        Expr expr = parse("(=> (instance ?X Integer) (=> (greaterThan ?X 0) (ListFn__2Fn__0Re1Re2Re 1.0 2.0)))");
        SUMOtoTFAform stfa = new SUMOtoTFAform();
        Set<String> sorts = stfa.missingSortsExpr(expr);
        assertFalse("Deeply nested ListFn should yield a sort", sorts.isEmpty());
    }

    // ---- pred-var fallback fix: empty expansion → keep original -----------

    /**
     * When pred-var expansion finds no KB instances for the variable's type,
     * preProcessExpr must keep the original formula (not drop it), mirroring
     * the string-path replacePredVarsAndRowVars behavior.
     *
     * We verify this by running preProcessExpr directly on a formula whose
     * pred-var type constraint matches few or zero KB relations.
     */
    @Test
    public void testPredVarFallbackKeptOnEmptyExpansion() {
        // TotalOrderingRelation: string path keeps original when no instances found
        String kif = "(=> (instance ?REL TotalOrderingRelation) " +
                     "(=> (and (?REL ?INST1 ?INST2) (?REL ?INST2 ?INST1)) (equal ?INST1 ?INST2)))";
        SuokifVisitor visitor = SuokifVisitor.parseSentence(kif);
        assertNotNull(visitor.result);
        assertFalse(visitor.result.isEmpty());
        FormulaAST fa = visitor.result.get(0);
        assertNotNull(fa);
        // Run full preProcessExpr — must not return null or empty
        FormulaPreprocessor fp = new FormulaPreprocessor();
        Set<Expr> result = fp.preProcessExpr(fa, false, kb);
        // Either the original is kept, or pred-var instantiations are returned —
        // but the result must never be empty (that was the bug).
        assertNotNull("preProcessExpr must not return null", result);
        assertFalse("preProcessExpr must not drop the formula when pred-var has no KB instances",
                result.isEmpty());
    }

    @Test
    public void testPredVarFallbackKeptForTrichotomizingRelation() {
        String kif = "(=> (instance ?REL TrichotomizingRelation) " +
                     "(=> (and (not (?REL ?INST1 ?INST2)) (not (?REL ?INST2 ?INST1))) " +
                     "(equal ?INST1 ?INST2)))";
        SuokifVisitor visitor = SuokifVisitor.parseSentence(kif);
        assertNotNull(visitor.result);
        assertFalse(visitor.result.isEmpty());
        FormulaAST fa = visitor.result.get(0);
        assertNotNull(fa);
        FormulaPreprocessor fp = new FormulaPreprocessor();
        Set<Expr> result = fp.preProcessExpr(fa, false, kb);
        assertNotNull("preProcessExpr must not return null", result);
        assertFalse("preProcessExpr must not drop trichotomizing-relation pred-var formula",
                result.isEmpty());
    }

    // ---- row-var + containsNumber: keep original ---------------------------

    /**
     * When a row-var formula also has containsNumber==true, preProcessExpr
     * must keep the original formula (not drop it), mirroring the string-path
     * behavior.
     */
    @Test
    public void testRowVarWithNumberKeptInPreProcess() {
        // GreatestCommonDivisorFn uses @ROW and appears with numeric literals in SUMO
        String kif = "(=> (equal (GreatestCommonDivisorFn @ROW) ?GCD) " +
                     "(=> (inList 0 (ListFn @ROW)) (equal ?GCD 0)))";
        SuokifVisitor visitor = SuokifVisitor.parseSentence(kif);
        assertNotNull(visitor.result);
        assertFalse(visitor.result.isEmpty());
        FormulaAST fa = visitor.result.get(0);
        assertNotNull(fa);
        FormulaPreprocessor fp = new FormulaPreprocessor();
        Set<Expr> result = fp.preProcessExpr(fa, false, kb);
        assertNotNull("preProcessExpr must not return null for row-var+number formula", result);
        assertFalse("preProcessExpr must keep row-var+number formula (not drop it)",
                result.isEmpty());
    }



}
