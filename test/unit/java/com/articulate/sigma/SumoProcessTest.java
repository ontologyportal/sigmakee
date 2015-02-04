package com.articulate.sigma;

import com.google.common.collect.*;
import org.junit.Test;

import java.util.Collection;

import static org.junit.Assert.assertEquals;

// Tests on FormulaPreprocessor that do not require KBs be loaded.
public class SumoProcessTest extends UnitTestBase {

    /**
     * KB parameter cannot be null.
     */
    @Test(expected=IllegalArgumentException.class)
    public void testInvalidKB() {
        new SumoProcess(null, "agent", "Process Human");
    }

    /**
     * role parameter must be a known role.
     */
    @Test(expected=IllegalArgumentException.class)
    public void testInvalidRole() {
        new SumoProcess(SigmaTestBase.kb, "invalidRole", "hi there");
    }

    /**
     * roleArguments parameter should consist of exactly two tokens.
     */
    @Test(expected=IllegalArgumentException.class)
    public void testInvalidRoleArgs1() {
        new SumoProcess(SigmaTestBase.kb, "agent", "invalidArgs");
    }

    /**
     * roleArguments parameter should consist of exactly two tokens.
     */
    @Test(expected=IllegalArgumentException.class)
    public void testInvalidRoleArgs2() {
        new SumoProcess(SigmaTestBase.kb, "agent", "invalidArgs no good");
    }

    /**
     * 1st token of roleArguments parameter should be a type of Process.
     */
    @Test(expected=IllegalArgumentException.class)
    public void testInvalidRoleArgs3() {
        new SumoProcess(SigmaTestBase.kb, "agent", "EatingBadTastingOatmeal John");
    }


    @Test
    public void testBasicSumoProcessFunctionality() {
        // Test constructor.
        SumoProcess process = new SumoProcess(SigmaTestBase.kb, "agent", "Driving Human");
        assertEquals(1, process.getAgents().size());
        assertEquals(0, process.getPatients().size());

        //Test agent getters and setters.
        Collection<String> newAgents = Sets.newHashSet("Tom", "Dick", "Harry");
        process.addAgents(Lists.newArrayList(newAgents));
        newAgents.add("Human");
        assertEquals(newAgents, process.getAgents());

        // Test patient getters and setters.
        Collection<String> newPatients = Sets.newHashSet("Automobile");
        process.addPatients(Lists.newArrayList(newPatients));
        assertEquals(newPatients, process.getPatients());

        // Test toString().
        String expected = "agent Driving Dick\n" +
                "agent Driving Harry\n" +
                "agent Driving Human\n" +
                "agent Driving Tom\n" +
                "patient Driving Automobile\n";
        assertEquals(expected, process.toString());

        // Test toNaturalLanguage().
        expected = "Dick drives.";
        assertEquals(expected, process.toNaturalLanguage());
    }

    @Test
    public void testAddMultipleRoles() {
        // Test constructor.
        SumoProcess process = new SumoProcess(SigmaTestBase.kb, "agent", "Driving Human");
        process.addRole(SumoProcess.ThematicRole.BENEFACTIVE, "Sally");
        process.addRole(SumoProcess.ThematicRole.GOAL, "HospitalBuilding");

        String expected = "agent Driving Human\n" +
                "goal Driving HospitalBuilding\n" +
                "benefactive Driving Sally\n";
        assertEquals(expected, process.toString());

    }

}