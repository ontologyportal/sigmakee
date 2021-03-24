package com.articulate.sigma;

import com.google.common.collect.Sets;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Set;
import java.util.Arrays;

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

        ArrayList<Formula> actual = SigmaTestBase.kb.askWithTwoRestrictions(0, "subclass", 1, "Driving", 2, "Guiding");
        assertNotEquals(0, actual.size());
    }

    /** ***************************************************************
     * Fails because askWithTwoRestrictions does not go up the class hierarchy but if caching is on will get "1".
     */
    @Test
    public void testAskWithTwoRestrictionsIndirect1() {

        ArrayList<Formula> actual = SigmaTestBase.kb.askWithTwoRestrictions(0, "subclass", 1, "Driving", 2, "Guiding");
        if (actual != null && actual.size() != 0)
            System.out.println("KBtest.testAskWithTwoRestrictionsIndirect1(): " + actual);
        assertEquals(1, actual.size());
    }

    /** ***************************************************************
     * Fails because askWithTwoRestrictions does not go up the class hierarchy.
     */
    @Test
    public void testAskWithTwoRestrictionsIndirect2() {

        ArrayList<Formula> actual = SigmaTestBase.kb.askWithTwoRestrictions(0, "subclass", 1, "Boy", 2, "Entity");
        assertEquals(0, actual.size());
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

}