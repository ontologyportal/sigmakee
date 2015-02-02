package com.articulate.sigma;

import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

public class HtmlFormatterTest extends SigmaTestBase {

    /**
     * This test replicates the two tests found in Htmlformatter.main( ).
     */
    @Test
    public void testTermIntersection()   {

        // Testing termIntersection().
        ArrayList<Formula> forms = KButilities.termIntersection(kb, "ShapeChange", "ShapeAttribute");

        String expectedFormula = "(=>" +
                " (and" +
                " (instance ?ALT ShapeChange)" +
                " (patient ?ALT ?OBJ))" +
                " (exists (?PROPERTY)" +
                " (and" +
                " (instance ?PROPERTY ShapeAttribute)" +
                " (or" +
                " (and" +
                " (holdsDuring" +
                " (BeginFn" +
                " (WhenFn ?ALT))" +
                " (attribute ?OBJ ?PROPERTY))" +
                " (holdsDuring" +
                " (EndFn" +
                " (WhenFn ?ALT))" +
                " (not" +
                " (attribute ?OBJ ?PROPERTY))))" +
                " (and" +
                " (holdsDuring" +
                " (BeginFn" +
                " (WhenFn ?ALT))" +
                " (not" +
                " (attribute ?OBJ ?PROPERTY)))" +
                " (holdsDuring" +
                " (EndFn" +
                " (WhenFn ?ALT))" +
                " (attribute ?OBJ ?PROPERTY)))))))";

        assertEquals(1, forms.size());

        Formula f = new Formula();
        f.read(expectedFormula);
        assertEquals(f.theFormula, forms.get(0).theFormula);

        // Testing formatFormulaList( )
        String actualHtml = HTMLformatter.formatFormulaList(forms,"",  kb, "EnglishLanguage",  "SUO-KIF", 0, 0, "");

        String expectedHtml = "<tr><td width=\"50%\" valign=\"top\">(=><br>\n" +
                "&nbsp;&nbsp;&nbsp;&nbsp;(and<br>\n" +
                "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;(instance ?ALT ShapeChange)<br>\n" +
                "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;(patient ?ALT ?OBJ))<br>\n" +
                "&nbsp;&nbsp;&nbsp;&nbsp;(exists (?PROPERTY)<br>\n" +
                "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;(and<br>\n" +
                "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;(instance ?PROPERTY ShapeAttribute)<br>\n" +
                "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;(or<br>\n" +
                "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;(and<br>\n" +
                "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;(holdsDuring<br>\n" +
                "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;(BeginFn<br>\n" +
                "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;(WhenFn ?ALT))<br>\n" +
                "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;(attribute ?OBJ ?PROPERTY))<br>\n" +
                "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;(holdsDuring<br>\n" +
                "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;(EndFn<br>\n" +
                "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;(WhenFn ?ALT))<br>\n" +
                "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;(not<br>\n" +
                "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;(attribute ?OBJ ?PROPERTY))))<br>\n" +
                "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;(and<br>\n" +
                "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;(holdsDuring<br>\n" +
                "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;(BeginFn<br>\n" +
                "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;(WhenFn ?ALT))<br>\n" +
                "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;(not<br>\n" +
                "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;(attribute ?OBJ ?PROPERTY)))<br>\n" +
                "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;(holdsDuring<br>\n" +
                "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;(EndFn<br>\n" +
                "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;(WhenFn ?ALT))<br>\n" +
                "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;(attribute ?OBJ ?PROPERTY)))))))</td>\n" +
                "<td width=\"10%\" valign=\"top\" bgcolor=\"#B8CADF\">Merge-2.kif 10436-10449</a></td>\n" +
                "<td width=\"40%\" valign=\"top\"><ul><li>If <a href=\"&term=Physical\">a  physical</a> is an <a href=\"&term=instance\">instance</a> of <a href=\"&term=ShapeChange\">shape change</a> and <a href=\"&term=Object\">an object</a> is a <a href=\"&term=patient\">patient</a> of <a href=\"&term=Physical\">the physical</a>,</li><li>then there exists <a href=\"&term=Entity\">an entity</a> such that <a href=\"&term=Entity\">the entity</a> is an <a href=\"&term=instance\">instance</a> of <a href=\"&term=ShapeAttribute\">shape attribute</a> and <a href=\"&term=Entity\">the entity</a> is an <a href=\"&term=attribute\">attribute</a> of <a href=\"&term=Object\">the object</a> holds <a href=\"&term=holdsDuring\">during</a> the <a href=\"&term=BeginFn\">beginning</a> of the <a href=\"&term=WhenFn\">time</a> of existence of <a href=\"&term=Physical\">the physical</a> and <a href=\"&term=Entity\">the entity</a> is not an <a href=\"&term=attribute\">attribute</a> of <a href=\"&term=Object\">the object</a> holds <a href=\"&term=holdsDuring\">during</a> the <a href=\"&term=EndFn\">end</a> of the <a href=\"&term=WhenFn\">time</a> of existence of <a href=\"&term=Physical\">the physical</a> or <a href=\"&term=Entity\">the entity</a> is not an <a href=\"&term=attribute\">attribute</a> of <a href=\"&term=Object\">the object</a> holds <a href=\"&term=holdsDuring\">during</a> the <a href=\"&term=BeginFn\">beginning</a> of the <a href=\"&term=WhenFn\">time</a> of existence of <a href=\"&term=Physical\">the physical</a> and <a href=\"&term=Entity\">the entity</a> is an <a href=\"&term=attribute\">attribute</a> of <a href=\"&term=Object\">the object</a> holds <a href=\"&term=holdsDuring\">during</a> the <a href=\"&term=EndFn\">end</a> of the <a href=\"&term=WhenFn\">time</a> of existence of <a href=\"&term=Physical\">the physical</a></li></ul></td></tr>\n";

        assertEquals(expectedHtml, actualHtml);
    }

}