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
import java.util.Set;

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

    /**
     * Dual-run: parse kif to Expr, call both process() and processExpr(),
     * assert they produce the same TFF string.
     */
    private void dualRun(String kif) {
        Expr expr = parse(kif);
        String strResult  = SUMOtoTFAform.process(kif, false);
        String exprResult = SUMOtoTFAform.processExpr(expr, false);
        System.out.println("kif:    " + kif);
        System.out.println("string: " + strResult);
        System.out.println("expr:   " + exprResult);
        assertEquals("processExpr / process mismatch for: " + kif, strResult, exprResult);
    }

    // ---- numeric domains ---------------------------------------------------

    @Test
    public void testDualRunAdditionFn() {
        dualRun("(equal (AdditionFn 1 2) 3)");
    }

    @Test
    public void testDualRunFloorFn() {
        dualRun("(equal (FloorFn 3.7) 3)");
    }

    @Test
    public void testDualRunDivisionFnMixed() {
        // DivisionFn with integer / real → mixed numeric type promotion
        dualRun("(equal (DivisionFn 10 3.0) 3.333)");
    }

    @Test
    public void testDualRunMultiplicationFn() {
        dualRun("(equal (MultiplicationFn 4 5) 20)");
    }

    @Test
    public void testDualRunSubtractionFn() {
        dualRun("(equal (SubtractionFn 10 3) 7)");
    }

    // ---- comparison operators ----------------------------------------------

    @Test
    public void testDualRunLessThan() {
        dualRun("(lessThan 3 4)");
    }

    @Test
    public void testDualRunGreaterThan() {
        dualRun("(greaterThan 5 2)");
    }

    @Test
    public void testDualRunLessThanOrEqualTo() {
        dualRun("(lessThanOrEqualTo 3 3)");
    }

    @Test
    public void testDualRunGreaterThanOrEqualTo() {
        dualRun("(greaterThanOrEqualTo 5 3)");
    }

    // ---- mixed numeric type ------------------------------------------------

    @Test
    public void testDualRunAbsoluteValueFn() {
        dualRun("(equal (AbsoluteValueFn -5) 5)");
    }

    @Test
    public void testDualRunExponentiationFn() {
        dualRun("(equal (ExponentiationFn 2 10) 1024)");
    }

    @Test
    public void testDualRunRealNumber() {
        dualRun("(equal (AdditionFn 1.5 2.5) 4.0)");
    }

    // ---- logic ops ---------------------------------------------------------

    @Test
    public void testDualRunImplicationWithNumeric() {
        dualRun("(=> (lessThan ?X 0) (lessThan (AdditionFn ?X 1) 1))");
    }

    @Test
    public void testDualRunForallWithNumeric() {
        dualRun("(forall (?X) (=> (lessThan ?X 0) (lessThan ?X 1)))");
    }

    @Test
    public void testDualRunExistsWithNumeric() {
        dualRun("(exists (?X) (and (instance ?X Integer) (lessThan ?X 0)))");
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

    // ---- end-to-end: processExpr on full preProcessExpr output -------------

    /**
     * Run the full pipeline: preProcessExpr → processExpr on each output,
     * compare each result against the string path's process().
     *
     * Uses a simple formula with no pred-vars or row-vars so the output is
     * deterministic and comparable.
     */
    @Test
    public void testFullPipelineDualRunAddition() {
        String kif = "(=> (instance ?N Integer) (equal (AdditionFn ?N 0) ?N))";
        Expr expr = parse(kif);
        String strResult  = SUMOtoTFAform.process(kif, false);
        String exprResult = SUMOtoTFAform.processExpr(expr, false);
        System.out.println("full pipeline kif:    " + kif);
        System.out.println("full pipeline string: " + strResult);
        System.out.println("full pipeline expr:   " + exprResult);
        assertEquals("Full pipeline dual-run mismatch", strResult, exprResult);
    }

    @Test
    public void testFullPipelineDualRunSubclass() {
        dualRun("(=> (subclass ?X Entity) (instance ?X SetOrClass))");
    }

    @Test
    public void testFullPipelineDualRunTripleQuantifier() {
        dualRun("(forall (?X ?Y ?Z) (=> (and (instance ?X Integer) " +
                "(instance ?Y Integer) (equal (AdditionFn ?X ?Y) ?Z)) " +
                "(instance ?Z Integer)))");
    }

    /**
     * Dual-run (string vs Expr) for the AbsoluteValueFn biconditional, with
     * structural assertions against the known TFF output from SUMO.tff.
     *
     * The preprocessing pipeline transforms the formula significantly:
     *  - (instance ?NUMBER1 NonnegativeRealNumber) → SignumFn checks (= 1 or = 0)
     *  - (instance ?NUMBER1 NegativeRealNumber)    → SignumFn check  (= -1)
     *  - SubtractionFn 0.0 ?NUMBER1               → $difference(0.0, ?NUMBER1)
     *  - <=>                                       → (A => B) & (B => A)
     *  - ?NUMBER1, ?NUMBER2 typed as $real
     *
     * Expected TFF (from SUMO.tff):
     *   ! [V__NUMBER1 : $real, V__NUMBER2 : $real] :
     *     (((s__AbsoluteValueFn(V__NUMBER1) = V__NUMBER2 =>
     *          (((s__SignumFn(V__NUMBER1) = 1 | s__SignumFn(V__NUMBER1) = 0) & V__NUMBER1 = V__NUMBER2)
     *         | (s__SignumFn(V__NUMBER1) = -1 & V__NUMBER2 = $difference(0.0, V__NUMBER1))))
     *       & ((((s__SignumFn(V__NUMBER1) = 1 | s__SignumFn(V__NUMBER1) = 0) & V__NUMBER1 = V__NUMBER2)
     *         | (s__SignumFn(V__NUMBER1) = -1 & V__NUMBER2 = $difference(0.0, V__NUMBER1)))
     *          => s__AbsoluteValueFn(V__NUMBER1) = V__NUMBER2)))
     */
    @Test
    public void testDualRunAbsoluteValueFnBiconditional() {
        String kif =
            "(<=> " +
            "  (and " +
            "    (equal (AbsoluteValueFn ?NUMBER1) ?NUMBER2) " +
            "    (instance ?NUMBER1 RealNumber) " +
            "    (instance ?NUMBER2 RealNumber)) " +
            "  (or " +
            "    (and " +
            "      (instance ?NUMBER1 NonnegativeRealNumber) " +
            "      (equal ?NUMBER1 ?NUMBER2)) " +
            "    (and " +
            "      (instance ?NUMBER1 NegativeRealNumber) " +
            "      (equal ?NUMBER2 (SubtractionFn 0.0 ?NUMBER1)))))";

        Expr expr = parse(kif);
        String strResult  = SUMOtoTFAform.process(kif, false);
        String exprResult = SUMOtoTFAform.processExpr(expr, false);

        System.out.println("AbsoluteValueFn biconditional");
        System.out.println("  kif:    " + kif);
        System.out.println("  string: " + strResult);
        System.out.println("  expr:   " + exprResult);

        assertNotNull("string path must produce a result", strResult);
        assertNotNull("expr path must produce a result", exprResult);
        assertEquals("string and expr TFF outputs must match", strResult, exprResult);

        // ---- structural assertions against the known SUMO.tff output ----

        // Variables must be universally quantified as $real
        assertTrue("must quantify NUMBER1 as $real", exprResult.contains("$real"));
        // AbsoluteValueFn must appear as a SUMO-prefixed function
        assertTrue("must contain s__AbsoluteValueFn", exprResult.contains("s__AbsoluteValueFn"));
        // NonnegativeRealNumber/NegativeRealNumber instance checks become SignumFn
        assertTrue("must contain s__SignumFn (instance checks lowered to signum)",
                exprResult.contains("s__SignumFn"));
        // SubtractionFn 0.0 becomes $difference
        assertTrue("must contain $difference (SubtractionFn lowered to TPTP arithmetic)",
                exprResult.contains("$difference"));
        // The real literal must be preserved
        assertTrue("must contain 0.0", exprResult.contains("0.0"));
        // Biconditional must be expanded to two implications
        long arrowCount = exprResult.chars().filter(c -> c == '>').count();
        assertTrue("biconditional must expand to at least two '=>' arrows (got " + arrowCount + ")",
                arrowCount >= 2);
    }
}
