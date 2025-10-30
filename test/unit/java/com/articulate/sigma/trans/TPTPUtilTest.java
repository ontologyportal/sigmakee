package com.articulate.sigma.trans;

import com.articulate.sigma.UnitTestBase;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class TPTPUtilTest extends UnitTestBase {

    @Test
    public void testClearProofFile() {
        List<String> input = Arrays.asList(
                "% Header comment",
                "",
                "fof(f1, axiom,",
                "    (p(a) => q(a))).",
                "  fof(f2, conjecture,",
                "      (q(a))).",
                "% Footer comment"
        );

        List<String> expected = Arrays.asList(
                "% Header comment",
                "fof(f1, axiom, (p(a) => q(a))).",
                "fof(f2, conjecture, (q(a))).",
                "% Footer comment"
        );

        List<String> actual = TPTPutil.clearProofFile(input);
        assertEquals(expected, actual);
    }


    @Test
    public void dropOnePremiseFormulasFOFTest() {
        List<String> input = Arrays.asList(
                "% Refutation found. Thanks to Tanya!",
                "% SZS status Theorem for min-problem",
                "% SZS output start Proof for min-problem",
                "fof(f1,axiom,( s__instance(s__wife__m,s__Relation)), file('/Users/Mac_1/workspace/sigmakee/min-problem.tptp',unknown)).",
                "fof(f2,conjecture,( ? [X0] : s__instance(X0,s__Relation)), file('/Users/Mac_1/workspace/sigmakee/min-problem.tptp',unknown)).",
                "fof(f3,negated_conjecture,( ~? [X0] : s__instance(X0,s__Relation)), inference(negated_conjecture,[status(cth)],[f2])).",
                "fof(f4,plain,( ! [X0] : ~s__instance(X0,s__Relation)), inference(ennf_transformation,[],[f3])).",
                "fof(f5,plain,( s__instance(s__wife__m,s__Relation)), inference(cnf_transformation,[],[f1])).",
                "fof(f6,plain,( ( ! [X0] : (~s__instance(X0,s__Relation)) )), inference(cnf_transformation,[],[f4])).",
                "fof(f7,plain,( $false), inference(resolution,[],[f5,f6])).",
                "% SZS output end Proof for min-problem"
        );

        List<String> expected = Arrays.asList(
                "% Refutation found. Thanks to Tanya!",
                "% SZS status Theorem for min-problem",
                "% SZS output start Proof for min-problem",
                "fof(1,axiom, (s__instance(s__wife__m,s__Relation)), file('/Users/Mac_1/workspace/sigmakee/min-problem.tptp',unknown)).",
                "fof(2,conjecture, (?[X0] : s__instance(X0,s__Relation)), file('/Users/Mac_1/workspace/sigmakee/min-problem.tptp',unknown)).",
                "fof(3,negated_conjecture, (~?[X0] : s__instance(X0,s__Relation)), inference(negated_conjecture,[status(cth)],[2])).",
                "fof(4,plain, ($false), inference(resolution,[],[1,2])).",
                "% SZS output end Proof for min-problem"
        );

        List<String> out = TPTPutil.dropOnePremiseFormulasFOF(input);

        // Keeps header/footer comments
        assertEquals("% Refutation found. Thanks to Tanya!", out.get(0));
        assertEquals("% SZS output start Proof for min-problem",out.get(2));
        assertEquals("% SZS output end Proof for min-problem", out.get(out.size() - 1));

        assertEquals(expected, out);
    }

}
