package com.articulate.sigma;

import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Set;

import static org.junit.Assert.assertEquals;

// Tests on SumoProcess that do not require KBs be loaded.

public class SumoProcessCollectorSimpleTest extends SigmaMockTestBase {

    protected KB knowledgeBase = this.kbMock;

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    // Testing for null/empty parameters.
    @Test(expected=IllegalArgumentException.class)
    public void testNullKB() {
        new SumoProcessCollector(null, "agent", "Process", "Human");
    }

    @Test(expected=IllegalArgumentException.class)
    public void testEmptyRole() {
        new SumoProcessCollector(knowledgeBase, "", "Process", "Human");
    }

    @Test(expected=IllegalArgumentException.class)
    public void testEmptyProcess() {
        new SumoProcessCollector(knowledgeBase, "agent", "", "Human");
    }

    @Test(expected=IllegalArgumentException.class)
    public void testEmptyEntity() {
        new SumoProcessCollector(knowledgeBase, "agent", "Process", "");
    }

    /**
     * role parameter must be a known role.
     */
    @Test
    public void testInvalidRole() {
        expectedEx.expect(IllegalArgumentException.class);
        expectedEx.expectMessage("Invalid role: role = invalidRole; process = hi; entity = there.");

        new SumoProcessCollector(knowledgeBase, "invalidRole", "hi", "there");
    }

    /**
     * process parameter must be a known process
     */
    @Test
    public void testInvalidProcess() {
        expectedEx.expect(IllegalArgumentException.class);
        expectedEx.expectMessage("Process parameter is not a Process: role = agent; process = EatingBadTastingOatmeal; entity = John.");

        new SumoProcessCollector(knowledgeBase, "agent", "EatingBadTastingOatmeal", "John");
    }

    @Test
    public void testBasicSumoProcessFunctionality() {
        // Test constructor.
        SumoProcessCollector process = new SumoProcessCollector(knowledgeBase, "agent", "Driving", "Human");
        assertEquals(1, process.getRolesAndEntities().size());
        assertEquals(1, process.getRoleEntities("agent").size());
        assertEquals(0, process.getRoleEntities("patient").size());
        assertEquals(true, process.isValid());

        //Test agent getters and setters.
        process.addRole("agent", "Tom");

        Set<String> expectedAgents = Sets.newTreeSet(Sets.newHashSet("Tom", "human"));

        Set<String> actualAgents = process.getRoleEntities("agent");
        //Collections.sort(actualAgents);

        assertEquals(expectedAgents, actualAgents);

        // Test patient getters and setters.
        process.addRole("patient", "Automobile");

        Set<String> expectedPatients = Sets.newTreeSet(Sets.newHashSet("automobile"));
        Set<String> actualPatients = process.getRoleEntities("patient");

        assertEquals(expectedPatients, actualPatients);

        // Test toString().
        String expected = "agent Driving Tom\n" +
                "agent Driving human\n" +
                "patient Driving automobile\n";
        assertEquals(expected, process.toString());
    }

    @Test
    public void testIsValidFalse() {
        SumoProcessCollector process = new SumoProcessCollector(knowledgeBase, "benefactive", "Driving", "Sally");
        process.addRole("goal", "HospitalBuilding");

        assertEquals(false, process.isValid());
    }

    @Test
    public void testAddMultipleRoles() {
        SumoProcessCollector process = new SumoProcessCollector(knowledgeBase, "agent", "Driving", "Human");
        process.addRole("benefactive", "Sally");
        process.addRole("goal", "HospitalBuilding");

        String expected = "agent Driving human\n" +
                "benefactive Driving Sally\n" +
                "goal Driving HospitalBuilding\n";
        assertEquals(expected, process.toString());
    }

    @Test
    public void testAddMultipleRolesWithVariables() {
        SumoProcessCollector process = new SumoProcessCollector(knowledgeBase, "agent", "Driving", "?H");
        process.addRole("patient", "?C");
        process.addRole("goal", "?P");

        String expected = "agent Driving ?H\n" +
                "goal Driving ?P\n" +
                "patient Driving ?C\n";
        assertEquals(expected, process.toString());
        expected = "?H drives ?C";
        assertEquals(expected, process.toNaturalLanguage());
    }

    /**
     * Verify that repeated, identical roles are ignored.
     */
    @Test
    public void testNoIdenticalRoleParticipants() {
        SumoProcessCollector process = new SumoProcessCollector(knowledgeBase, "agent", "Driving", "Mark");
        assertEquals(1, process.getRoleEntities("agent").size());

        process.addRole("agent", "Mark");
        assertEquals(1, process.getRoleEntities("agent").size());
    }

    /**
     * Verify that when you ask for a copy of the roles, you can't use it to change the object's copy.
     */
    @Test
    public void testDeepCopies() {
        SumoProcessCollector process = new SumoProcessCollector(knowledgeBase, "agent", "Driving", "Mark");
        process.addRole("goal", "HospitalBuilding");

        assertEquals(1, process.getRoleEntities("agent").size());
        assertEquals(2, process.getRolesAndEntities().size());

        // Get local copy of agents, and change that.
        Set<String> agents = process.getRoleEntities("agent");
        agents.add("Sally");
        assertEquals(1, process.getRoleEntities("agent").size());

        // Get local copy of all roles, and change that.
        Multimap<String, String> allRoles = process.getRolesAndEntities();
        allRoles.put("goal", "House");
        assertEquals(2, process.getRolesAndEntities().size());
    }

    @Test
    public void testFormulateNaturalDirectObject() {
        SumoProcessCollector process = new SumoProcessCollector(knowledgeBase, "patient", "Driving", "Automobile");

        String expected = "an automobile";
        assertEquals(expected, process.formulateNaturalDirectObject());

        process.addRole("patient", "Truck");
        expected = "an automobile and a truck";
        assertEquals(expected, process.formulateNaturalDirectObject());

        process.addRole("patient", "Taxi");
        expected = "an automobile and a taxi and a truck";
        assertEquals(expected, process.formulateNaturalDirectObject());
    }

    /**
     * Verifies that, at least for the time being, the case of "Entity" participants is not changed, and we do not precede
     * it with a definite article.
     */
    @Test
    public void testFormulateNaturalDirectObjectEntity() {
        SumoProcessCollector process = new SumoProcessCollector(knowledgeBase, "patient", "Driving", "Entity");

        String expected = "Entity";
        assertEquals(expected, process.formulateNaturalDirectObject());
    }

    @Test
    public void testFormulateNaturalSubject() {
        SumoProcessCollector process = new SumoProcessCollector(knowledgeBase, "agent", "Driving", "Human");

        String expected = "a human";
        assertEquals(expected, process.formulateNaturalSubject());

        process.addRole("agent", "Coffee");
        expected = "coffee and a human";
        assertEquals(expected, process.formulateNaturalSubject());

        process.addRole("agent", "MedicalDoctor");
        expected = "coffee and a human and a medicaldoctor";
        assertEquals(expected, process.formulateNaturalSubject());
    }

    @Test
    public void testFormulateNaturalVerbNotInWordNet() {
        SumoProcessCollector process = new SumoProcessCollector(knowledgeBase, "agent", "Transportation", "Maria");

        String expected = "performs a transportation";
        assertEquals(expected, process.formulateNaturalVerb());
    }


}