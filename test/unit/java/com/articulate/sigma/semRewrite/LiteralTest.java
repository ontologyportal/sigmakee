package com.articulate.sigma.semRewrite;

import com.articulate.sigma.UnitTestBase;
import org.junit.Test;

/**
 * Created by apease on 6/5/15.
 */
public class LiteralTest extends UnitTestBase {

    /****************************************************************
     */
    @Test
    public void testLiteral() {

        String example4 = "root(ROOT-0, reached-14), prepc_after(reached-14, making-2), " +
            "amod(stops-4, several-3), dobj(making-2, stops-4), nn(islands-7, Caribbean-6), " +
            "prep_at(making-2, islands-7), appos(islands-7, de-9), nsubj(reached-14, Leon-10), " +
            "poss(men-13, his-12), conj_and(Leon-10, men-13), nsubj(reached-14, men-13), " +
            "det(coast-17, the-15), amod(coast-17, east-16), dobj(reached-14, coast-17), " +
            "prep_of(coast-17, Florida-19), nn(Augustine-22, St.-21), appos(Florida-19, Augustine-22), " +
            "prep_on(reached-14, April-25), num(April-25, 2-26), num(April-25, 1513-28)";
        Lexer lex = new Lexer(example4);
        Literal.parse(lex,0);

        String example = "root(ROOT-0, than-15), mark(1/1000th-5, Although-1)";
        lex = new Lexer(example);
        Literal.parse(lex,0);

        example = "root(ROOT-0, than-15), mark(3,200, Although-1)";
        lex = new Lexer(example);
        Literal.parse(lex,0);
    }
}
