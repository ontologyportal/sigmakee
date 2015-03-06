package com.articulate.sigma.nlg;

import com.articulate.sigma.KB;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.junit.*;
import org.junit.rules.ExpectedException;

import java.util.Set;

import static org.junit.Assert.assertEquals;

// Tests on SumoProcess that do not require KBs be loaded.

public class SumoProcessCollectorSimpleTest extends SigmaMockTestBase {

    private final KB knowledgeBase = this.kbMock;

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
        Multimap<CaseRole, String> roleScratchPad = process.createNewRoleScratchPad();
        assertEquals(1, process.getRolesAndEntities().size());
        assertEquals(1, Sentence.getRoleEntities(CaseRole.AGENT, roleScratchPad).size());
        assertEquals(0, Sentence.getRoleEntities(CaseRole.PATIENT, roleScratchPad).size());

        //Test agent getters and setters.
        process.addRole("agent", "Tom");
        roleScratchPad = process.createNewRoleScratchPad();

        Set<String> expectedAgents = Sets.newTreeSet(Sets.newHashSet("Tom", "human"));

        Set<String> actualAgents = Sentence.getRoleEntities(CaseRole.AGENT, roleScratchPad);
        //Collections.sort(actualAgents);

        assertEquals(expectedAgents, actualAgents);

        // Test patient getters and setters.
        process.addRole("patient", "Automobile");
        roleScratchPad = process.createNewRoleScratchPad();

        Set<String> expectedPatients = Sets.newTreeSet(Sets.newHashSet("automobile"));
        Set<String> actualPatients = Sentence.getRoleEntities(CaseRole.PATIENT, roleScratchPad);

        assertEquals(expectedPatients, actualPatients);

        // Test toString().
        String expected = "agent Driving Tom\n" +
                "agent Driving human\n" +
                "patient Driving automobile\n";
        assertEquals(expected, process.toString());
    }

    /**
     * Ignoring test till we figure out how to do benefactive/benefits in SUMO.
     */
    @Ignore
    @Test
    public void testIsValidFalse() {
        SumoProcessCollector process = new SumoProcessCollector(knowledgeBase, "benefactive", "Driving", "Sally");
        process.addRole("goal", "HospitalBuilding");
    }

    @Test
    public void testAddMultipleRoles() {
        SumoProcessCollector process = new SumoProcessCollector(knowledgeBase, "agent", "Driving", "Human");
        process.addRole("patient", "Sally");
        process.addRole("destination", "HospitalBuilding");

        String expected = "agent Driving human\n" +
                "destination Driving HospitalBuilding\n" +
                "patient Driving Sally\n";
        assertEquals(expected, process.toString());
    }

    @Test
    public void testAddMultipleRolesWithVariables() {
        SumoProcessCollector process = new SumoProcessCollector(knowledgeBase, "agent", "Driving", "?H");
        process.addRole("patient", "?C");
        process.addRole("destination", "?P");

        String expected = "agent Driving ?H\n" +
                "destination Driving ?P\n" +
                "patient Driving ?C\n";
        assertEquals(expected, process.toString());
        expected = "?H drives ?C to ?P";
        assertEquals(expected, process.toNaturalLanguage());
    }

    /**
     * Verify that repeated, identical roles are ignored.
     */
    @Test
    public void testNoIdenticalRoleParticipants() {
        SumoProcessCollector process = new SumoProcessCollector(knowledgeBase, "agent", "Driving", "Mark");
        Multimap<CaseRole, String> roleScratchPad = process.createNewRoleScratchPad();
        assertEquals(1, Sentence.getRoleEntities(CaseRole.AGENT, roleScratchPad).size());

        process.addRole("agent", "Mark");
        roleScratchPad = process.createNewRoleScratchPad();
        assertEquals(1, Sentence.getRoleEntities(CaseRole.AGENT, roleScratchPad).size());
    }

    /**
     * Verify that when you ask for a copy of the roles, you can't use it to change the object's copy.
     */
    @Test
    public void testDeepCopies() {
        SumoProcessCollector process = new SumoProcessCollector(knowledgeBase, "agent", "Driving", "Mark");
        process.addRole("destination", "HospitalBuilding");
        Multimap<CaseRole, String> roleScratchPad = process.createNewRoleScratchPad();

        assertEquals(1, Sentence.getRoleEntities(CaseRole.AGENT, roleScratchPad).size());
        assertEquals(2, process.getRolesAndEntities().size());

        // Get local copy of agents, and change that.
        Set<String> agents = Sentence.getRoleEntities(CaseRole.AGENT, roleScratchPad);
        agents.add("Sally");
        assertEquals(1, Sentence.getRoleEntities(CaseRole.AGENT, roleScratchPad).size());

        // Get local copy of all roles, and change that.
        Multimap<CaseRole, String> allRoles = process.getRolesAndEntities();
        allRoles.put(CaseRole.DESTINATION, "House");
        assertEquals(2, process.getRolesAndEntities().size());
    }

    /**
     * Verify that createNewRoleScratchPad( ) returns a defensive copy.
     */
    @Test
    public void testCreateNewRoleScratchPad() {
        SumoProcessCollector process = new SumoProcessCollector(knowledgeBase, "agent", "Transportation", "Maria");

        Multimap<CaseRole,String> originalMap = process.createNewRoleScratchPad();
        assertEquals(1, originalMap.size());
        originalMap.clear();
        assertEquals(0, originalMap.size());

        Multimap<CaseRole,String> actualMap = process.createNewRoleScratchPad();

        assertEquals(1, actualMap.size());
    }


}