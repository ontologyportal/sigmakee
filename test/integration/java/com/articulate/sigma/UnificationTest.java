package com.articulate.sigma;

import com.articulate.sigma.semRewrite.CNF;
import com.articulate.sigma.semRewrite.Interpreter;
import com.articulate.sigma.semRewrite.Lexer;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertThat;

/**
 * Created by areed on 3/30/15.
 */
public class UnificationTest extends IntegrationTestBase {

    @Test
    public void testUnifyWhatDoesMaryDo() {
        Interpreter interp = new Interpreter();
        interp.initialize();

        String input = "root(ROOT-0,do-4), dobj(do-4,what-1), aux(do-4,do-2), nsubj(do-4,Mary-3), sumo(IntentionalProcess,do-2), names(Mary-3,\"Mary\"), attribute(Mary-3,Female), sumo(Human,Mary-3), number(SINGULAR,Mary-3)";
        Lexer lex = new Lexer(input);
        CNF cnfInput = CNF.parseSimple(lex);
        ArrayList<CNF> inputs = new ArrayList<CNF>();
        inputs.add(cnfInput);

        String[] expected = {
                "(agent ?X Mary-3)",
                "(attribute Mary-3 Female)",
                "(names Mary-3 \"Mary\")",
                "(instance Mary-3 Human)"
        };

        List<String> results = interp.interpretCNF(inputs);
        assertThat(results, hasItems(expected));
    }

    @Test
    public void testUnifyWhatDoesMaryKick() {
        Interpreter interp = new Interpreter();
        interp.initialize();

        String input = "root(ROOT-0,kick-2), nsubj(kick-2,Mary-1), det(cart-4,the-3), dobj(kick-2,cart-4), names(Mary-1,\"Mary\"), sumo(Wagon,cart-4), sumo(Kicking,kick-2), attribute(Mary-1,Female), sumo(Human,Mary-1), number(SINGULAR,Mary-1), tense(PAST,kick-2), number(SINGULAR,cart-4)";
        Lexer lex = new Lexer(input);
        CNF cnfInput = CNF.parseSimple(lex);
        ArrayList<CNF> inputs = new ArrayList<CNF>();
        inputs.add(cnfInput);

        String[] expected = {
                "(attribute Mary-1 Female)",
                "(instance cart-4 Wagon)",
                "(names Mary-1 \"Mary\")",
                "(patient kick-2 cart-4)",
                "(instance kick-2 Kicking)",
                "(instance Mary-1 Human)",
                "(agent kick-2 Mary-1)"
        };

        List<String> results = interp.interpretCNF(inputs);
        assertThat(results, hasItems(expected));
    }
}
