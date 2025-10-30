package com.articulate.sigma;

import com.articulate.sigma.tp.Vampire;
import com.articulate.sigma.trans.TPTP3ProofProcessor;

import com.google.common.collect.Sets;

import java.util.Set;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import static org.junit.Assert.*;

public class KBTest extends UnitTestBase {

    /** ***************************************************************
     */
    @Test
    public void testMostSpecificTerm() {

        String t = SigmaTestBase.kb.mostSpecificTerm(Arrays.asList(new String[]{"Entity","RealNumber"}));
        System.out.println("testMostSpecificTerm(): " + t);
        assertEquals("RealNumber", t);
    }

    /** ***************************************************************
     */
    @Test
    public void testAskWithTwoRestrictionsDirect1() {

        List<Formula> actual = SigmaTestBase.kb.askWithTwoRestrictions(0, "subclass", 1, "Driving", 2, "Guiding");
        assertNotEquals(0, actual.size());
    }

    /** ***************************************************************
     * Fails because askWithTwoRestrictions does not go up the class hierarchy but if caching is on will get "1".
     */
    @Test
    public void testAskWithTwoRestrictionsIndirect1() {

        List<Formula> actual = SigmaTestBase.kb.askWithTwoRestrictions(0, "subclass", 1, "Driving", 2, "Guiding");
        if (actual != null && !actual.isEmpty()) {
            System.out.println("KBtest.testAskWithTwoRestrictionsIndirect1(): " + actual);
            assertEquals(1, actual.size());
        }
    }

    /** ***************************************************************
     * Fails because askWithTwoRestrictions does not go up the class hierarchy.
     */
    @Test
    public void testAskWithTwoRestrictionsIndirect2() {

        List<Formula> actual = SigmaTestBase.kb.askWithTwoRestrictions(0, "subclass", 1, "Boy", 2, "Entity");
        assertEquals(0, actual.size());
    }

    /** ***************************************************************
     * test deleteUserAssertionsAndReload() -- with Vampire
     */
    @Test
    public void testDeleteUserAssertionsAndReloadWithVampire() {

        System.out.println("============== testDeleteUserAssertionsAndReloadWithVampire =====================");
        SigmaTestBase.kb.tell("(instance JohnJacob Human)");
        String query = "(instance JohnJacob Human)";
        Vampire vamp = SigmaTestBase.kb.askVampire(query,10,1);
        if (vamp != null)
            System.out.println("testDeleteUserAssertionsAndReloadWithVampire(): results: " + vamp.output);
        else
            System.out.println("testDeleteUserAssertionsAndReloadWithVampire(): results: " + null);
        TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
        if (vamp != null)
            vamp.output = TPTP3ProofProcessor.joinNreverseInputLines(vamp.output);
            tpp.parseProofOutput(vamp.output,query,kb,new StringBuilder());
        if (tpp.proof != null && (tpp.status.equals("Refutation") || tpp.status.equals("Theorem")))
            System.out.println("testDeleteUserAssertionsAndReloadWithVampire(1): success");
        else
            System.err.println("testDeleteUserAssertionsAndReloadWithVampire(1): fail, proof size: "+ tpp.proof.size() + " '" + tpp.status + "'");
        assertTrue(tpp.proof != null && (tpp.status.equals("Refutation") || tpp.status.equals("Theorem")));
        SigmaTestBase.kb.deleteUserAssertionsAndReload();
        vamp = SigmaTestBase.kb.askVampire(query,10,1);
        vamp.output = TPTP3ProofProcessor.joinNreverseInputLines(vamp.output);
        tpp.parseProofOutput(vamp.output, query, kb, new StringBuilder());
        System.out.println("User assertions deleted");
        System.out.println("testDeleteUserAssertionsAndReloadWithVampire(): results after delete: " + vamp);
        if (tpp.proof == null || tpp.status.equals("Timeout"))
            System.out.println("testDeleteUserAssertionsAndReloadWithVampire(2): success");
        else
            System.err.println("testDeleteUserAssertionsAndReloadWithVampire(2): fail, proof size: " + tpp.proof.size() + " '" + tpp.status + "'");
        assertTrue(tpp.proof == null || tpp.status.equals("Timeout"));
    }

    /** ***************************************************************
     * test deleteUserAssertionsAndReload()
     */
    @Test
    public void testDeleteUserAssertionsAndReload() {

        System.out.println("============== testDeleteUserAssertionsAndReload =====================");
        SigmaTestBase.kb.tell("(instance JohnJacob Human)");
        List<Formula> results = SigmaTestBase.kb.ask("arg",1,"JohnJacob");
        System.out.println("testDeleteUserAssertionsAndReload(): results: " + results);
        assertEquals(1, results.size());
        SigmaTestBase.kb.deleteUserAssertionsAndReload();
        results = SigmaTestBase.kb.ask("arg",1,"JohnJacob");
        System.out.println("User assertions deleted");
        System.out.println("testDeleteUserAssertionsAndReload(): results after delete: " + results);
        assertEquals(0, results.size());
    }

    /** ***************************************************************
     */
    @Test
    public void testIsSubclass2()   {
        assertTrue(SigmaTestBase.kb.isSubclass("Driving", "Process"));
    }

    /** ***************************************************************
     */
    @Test
    public void testRemoveSuperClassesEmptyInput() {

        Set<String> inputSet = Sets.newHashSet();
        Set<String> actualSet = SigmaTestBase.kb.removeSuperClasses(inputSet);
        Set<String> expectedSet = Sets.newHashSet();
        assertEquals(expectedSet, actualSet);
    }

    /** ***************************************************************
     */
    @Test
    public void testRemoveSuperClassesOneElementInput() {

        Set<String> inputSet = Sets.newHashSet("nonsenseWord");
        Set<String> actualSet = SigmaTestBase.kb.removeSuperClasses(inputSet);
        Set<String> expectedSet = Sets.newHashSet("nonsenseWord");
        assertEquals(expectedSet, actualSet);
    }

    /** ***************************************************************
     */
    @Test
    public void testRemoveSuperClassesTwoElementIdenticalInput1() {

        Set<String> inputSet = Sets.newHashSet("Entity", "Entity");
        Set<String> actualSet = SigmaTestBase.kb.removeSuperClasses(inputSet);
        Set<String> expectedSet = Sets.newHashSet("Entity");
        assertEquals(expectedSet, actualSet);
    }

    /** ***************************************************************
     */
    @Test
    public void testRemoveSuperClassesTwoElementIdenticalInput2() {

        Set<String> inputSet = Sets.newHashSet("Process", "Process");
        Set<String> actualSet = SigmaTestBase.kb.removeSuperClasses(inputSet);
        Set<String> expectedSet = Sets.newHashSet("Process");
        assertEquals(expectedSet, actualSet);
    }

    /** ***************************************************************
     */
    @Test
    public void testRemoveSuperClassesTwoElementIdenticalInput3() {

        Set<String> inputSet = Sets.newHashSet("Physical", "Physical");
        Set<String> actualSet = SigmaTestBase.kb.removeSuperClasses(inputSet);
        Set<String> expectedSet = Sets.newHashSet("Physical");
        assertEquals(expectedSet, actualSet);
    }

    /** ***************************************************************
     */
    @Test
    public void testRemoveSuperClassesTwoElementInput() {

        Set<String> inputSet = Sets.newHashSet("Man", "Human");
        Set<String> actualSet = SigmaTestBase.kb.removeSuperClasses(inputSet);
        Set<String> expectedSet = Sets.newHashSet("Man");
        assertEquals(expectedSet, actualSet);
    }

    /** ***************************************************************
     */
    @Test
    public void testRemoveSuperClassesTwoElementInputReverse() {

        Set<String> inputSet = Sets.newHashSet("Human", "Man");
        Set<String> actualSet = SigmaTestBase.kb.removeSuperClasses(inputSet);
        Set<String> expectedSet = Sets.newHashSet("Man");
        assertEquals(expectedSet, actualSet);
    }

    /** ***************************************************************
     */
    @Test
    public void testRemoveSuperClassesTwoElementInputNoSubclass() {

        Set<String> inputSet = Sets.newHashSet("Man", "Woman");
        Set<String> actualSet = SigmaTestBase.kb.removeSuperClasses(inputSet);
        Set<String> expectedSet = Sets.newHashSet("Man", "Woman");
        assertEquals(expectedSet, actualSet);
    }

    /** ***************************************************************
     */
    @Test
    public void testRemoveSuperClassesFiveElementInput() {

        Set<String> inputSet = Sets.newHashSet("Object", "CorpuscularObject", "Woman", "Human", "Man");
        Set<String> actualSet = SigmaTestBase.kb.removeSuperClasses(inputSet);
        Set<String> expectedSet = Sets.newHashSet("Man", "Woman");
        assertEquals(expectedSet, actualSet);
    }

    /** ***************************************************************
     */
    @Test
    public void testTermFormatMapAll() {

        System.out.println("============== testTermFormatMapAll =====================");
        System.out.println("Testing  kb.termFormatMapAll('EnglishLanguage')");
        Map<String, List<String>> termFormats = kb.getTermFormatMapAll("EnglishLanguage");
        List<String> motherTermFormats = termFormats.get("mother");
        for (String termFormat : motherTermFormats) {
            System.out.println("Term Format for mother: " + termFormat);
        }
        assertFalse(motherTermFormats.isEmpty());
    }

    /** ***************************************************************
     */
    @Test
    public void testFormatMapAll() {

        System.out.println("============== testFormatMapAll =====================");
        System.out.println("Testing  kb.formatMapAll('EnglishLanguage')");
        Map<String, List<String>> allFormats = kb.getFormatMapAll("EnglishLanguage");
        List<String> motherFormats = allFormats.get("mother");
        for (String format : motherFormats) {
            System.out.println("Format for mother: " + format);
        }
        assertFalse(motherFormats.isEmpty());
    }

}