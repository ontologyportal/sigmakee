package com.articulate.sigma;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.junit.Ignore;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * FormulaPreprocessor tests not focused on findExplicitTypes( ), but requiring that the KBs be loaded.
 */
public class FormulaPreprocessorIntegrationTest extends IntegrationTestBase {

    /**
     * NOTE: If this test fails, you need to load Mid-level-ontology.kif. One way to do this would be to edit
     * your config.xml file by putting this line under "<kb name="SUMO" >":
     *    <constituent filename=".../Mid-level-ontology.kif" />
     */
    @Test
    public void testComputeVariableTypesTypicalPart() {

        String stmt =   "(=> " +
                            "(typicalPart ?X ?Y) " +
                            "(subclass ?Y Object))";

        Formula f = new Formula();
        f.read(stmt);

        FormulaPreprocessor formulaPre = new FormulaPreprocessor();
        HashMap<String, HashSet<String>> actual = formulaPre.computeVariableTypes(f, SigmaTestBase.kb);

        Map<String, HashSet<String>> expected = Maps.newHashMap();
        HashSet<String> set1 = Sets.newHashSet("Class", "Object+");
        expected.put("?Y", set1);
        expected.put("?X", Sets.newHashSet("Object+"));

        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testFindTypes2() {

        Map<String, HashSet<String>> expected = Maps.newHashMap();
        expected.put("?NOTPARTPROB", Sets.newHashSet("RealNumber"));
        expected.put("?PART", Sets.newHashSet("Class", "Object+"));
        expected.put("?PARTPROB", Sets.newHashSet("RealNumber"));
        expected.put("?X", Sets.newHashSet("Entity"));
        expected.put("?WHOLE", Sets.newHashSet("Object+"));

        String strf = "(=> (and (typicalPart ?PART ?WHOLE) (instance ?X ?PART) " +
                "(equal ?PARTPROB (ProbabilityFn (exists (?Y) (and " +
                "(instance ?Y ?WHOLE) (part ?X ?Y))))) (equal ?NOTPARTPROB " +
                "(ProbabilityFn (not (exists (?Z) (and (instance ?Z ?WHOLE) " +
                "(part ?X ?Z))))))) (greaterThan ?PARTPROB ?NOTPARTPROB))";
        Formula f = new Formula();
        f.read(strf);
        FormulaPreprocessor fp = new FormulaPreprocessor();

        HashMap<String, HashSet<String>> actualMap = fp.computeVariableTypes(f, SigmaTestBase.kb);

        assertEquals(expected, actualMap);
    }

    /** ***************************************************************
     */
    @Ignore
    @Test
    public void testAddTypes3() {

        String strf = "(=> (and (typicalPart ?PART ?WHOLE) (instance ?X ?PART) " +
                "(equal ?PARTPROB (ProbabilityFn (exists (?Y) (and " +
                "(instance ?Y ?WHOLE) (part ?X ?Y))))) (equal (?NOTPARTPROB " +
                "(ProbabilityFn (not (exists (?Z) (and (instance ?Z ?WHOLE) " +
                "(part ?X ?Z))))))) (greaterThan ?PARTPROB ?NOTPARTPROB))";
        Formula f = new Formula();
        f.read(strf);
        FormulaPreprocessor fp = new FormulaPreprocessor();

        Formula expected = new Formula();
        String expectedString = "(=> (and (instance ?PART Class) (subclass ?PART Object) (instance ?PARTPROB Entity) (instance ?X Object) (instance ?WHOLE Class) (subclass ?WHOLE Object) (instance ?Y Object)) " +
                "(=> (and (typicalPart ?PART ?WHOLE) (instance ?X ?PART) " +
                "(equal ?PARTPROB (ProbabilityFn (exists (?Y) (and (instance ?Y ?WHOLE) (part ?X ?Y)))))" +
                "(equal (?NOTPARTPROB (ProbabilityFn (not (exists (?Z) (and (instance ?Z ?WHOLE) (part ?X ?Z))))))) " +
                "(greaterThan ?PARTPROB ?NOTPARTPROB))) ";
        expected.read(expectedString);
        Formula actual = fp.addTypeRestrictions(f, SigmaTestBase.kb);
        //assertTrue("expected: " + expected.toString() + ", but was: " + actual.toString(), expected.equals(actual));
        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testComputeVariableTypesPlaintiff() {

        String stmt =   "(exists (?P ?H)\n" +
                "           (and\n" +
                "               (instance ?P LegalAction)\n" +
                "               (instance ?H Human)\n" +
                "               (plaintiff ?P ?H)))";
        Formula f = new Formula();
        f.read(stmt);

        FormulaPreprocessor formulaPre = new FormulaPreprocessor();
        HashMap<String, HashSet<String>> actual = formulaPre.computeVariableTypes(f, SigmaTestBase.kb);

        Map<String, HashSet<String>> expected = Maps.newHashMap();
        HashSet<String> set1 = Sets.newHashSet("CognitiveAgent");
        expected.put("?H", set1);
        HashSet<String> set2 = Sets.newHashSet("LegalAction");
        expected.put("?P", set2);

        assertEquals(expected, actual);
    }

}