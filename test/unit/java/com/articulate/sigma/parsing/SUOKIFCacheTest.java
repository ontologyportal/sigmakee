package com.articulate.sigma.parsing;

import com.articulate.sigma.Formula;
import com.articulate.sigma.UnitTestBase;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class SUOKIFCacheTest extends UnitTestBase {

    public static Map<Integer, FormulaAST> process(String input) {

        System.out.println(input);
        SuokifVisitor.parseString(input);
        Map<Integer,FormulaAST> hm = SuokifVisitor.result;
        return hm;
    }

    /** ***************************************************************
     */
    @Test
    public void test1() {

        System.out.println("===================== SUOKIFCacheTest.test1() =====================");
        String input = "(likes John Mary)";
        Map<Integer,FormulaAST> hm = process(input);
        Formula f = hm.values().iterator().next();
        f.printCaches();
        System.out.println("termCache: " + f.termCache);
        String expected = "[John, likes, Mary]";
        assertEquals(expected,f.termCache.toString());
        System.out.println();
    }

    /** ***************************************************************
     */
    @Test
    public void test2() {

        System.out.println("===================== SUOKIFCacheTest.test2() =====================");
        String input = "(=> (and (minValue ?R ?ARG ?N) (?R @ARGS) (equal ?VAL (ListOrderFn (ListFn @ARGS) ?ARG))) (greaterThan ?VAL ?N))";
        Map<Integer,FormulaAST> hm = process(input);
        FormulaAST f = hm.values().iterator().next();
        f.printCaches();
        String expected = "[minValue, ListOrderFn, ListFn, greaterThan]";
        System.out.println("SUOKIFCacheText.test2(): expected term cache: " + expected);
        System.out.println("SUOKIFCacheText.test2(): actual term cache: " + f.termCache.toString());
        assertEquals(expected,f.termCache.toString());
        expected = "[@ARGS]";
        System.out.println("SUOKIFCacheText.test2(): expected row var cache: " + expected);
        System.out.println("SUOKIFCacheText.test2(): actual row var cache: " + f.rowVarCache.toString());
        assertEquals(expected,f.rowVarCache.toString());
        expected = "\tListOrderFn\t1: (ListFn@ARGS), 2: ?ARG, \n";
        StringBuilder sb = new StringBuilder();
        String pred = "ListOrderFn";
        sb.append("\t").append(pred).append("\t");
        for (Integer i : f.argMap.get(pred).keySet()) {
            sb.append(i).append(": ");
            for (SuokifParser.ArgumentContext c : f.argMap.get(pred).get(i)) {
                sb.append(c.getText()).append(", ");
            }
        }
        sb.append("\n");
        assertEquals(expected,sb.toString());
        System.out.println();
    }

    /** ***************************************************************
     */
    @Test
    public void test3() {

        System.out.println("===================== SUOKIFCacheTest.test3() =====================");
        String input = "(=>\n" +
                "    (and\n" +
                "        (attribute ?SYLLABLE Stressed)\n" +
                "        (instance ?WORD Word)\n" +
                "        (part ?SYLLABLE ?WORD))\n" +
                "    (not\n" +
                "        (exists (?SYLLABLE2)\n" +
                "            (and\n" +
                "                (instance ?SYLLABLE2 Syllable)\n" +
                "                (part ?SYLLABLE2 ?WORD)\n" +
                "                (attribute ?SYLLABLE2 Stressed)\n" +
                "                (not\n" +
                "                    (equal ?SYLLABLE2 ?SYLLABLE))))))";
        Map<Integer,FormulaAST> hm = process(input);
        FormulaAST f = hm.values().iterator().next();
        f.printCaches();
        String expected = "[?SYLLABLE2]";
        assertEquals(expected,f.existVarsCache.toString());
        assertEquals(expected,f.quantVarsCache.toString());
        expected = "[?WORD, ?SYLLABLE]";
        assertEquals(expected,f.unquantVarsCache.toString());
        System.out.println();
    }

    /** ***************************************************************
     */
    @Test
    public void test4() {

        System.out.println("===================== SUOKIFCacheTest.test4() =====================");
        String input = "(attribute ?SYLLABLE Stressed) ;; I am an in-line SUO-KIF comment\n";
        Map<Integer, FormulaAST> hm = process(input);
        FormulaAST f = hm.values().iterator().next();
        f.printCaches();

        assertFalse("Is not a comment",f.comment);
        System.out.println();
    }
}
