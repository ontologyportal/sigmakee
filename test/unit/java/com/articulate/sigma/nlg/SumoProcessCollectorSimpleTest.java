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
    public ExpectedException expectedException = ExpectedException.none();

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
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("Invalid role: role = invalidRole; process = hi; entity = there.");

        new SumoProcessCollector(knowledgeBase, "invalidRole", "hi", "there");
    }

    /**
     * process parameter must be a known process
     */
    @Test
    public void testInvalidProcess() {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("Process parameter is not a Process: role = agent; process = EatingBadTastingOatmeal; entity = John.");

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

        Set<String> expectedAgents = Sets.newTreeSet(Sets.newHashSet("Human", "Tom"));

        Set<String> actualAgents = Sentence.getRoleEntities(CaseRole.AGENT, roleScratchPad);
        //Collections.sort(actualAgents);

        assertEquals(expectedAgents, actualAgents);

        // Test patient getters and setters.
        process.addRole("patient", "Automobile");
        roleScratchPad = process.createNewRoleScratchPad();

        Set<String> expectedPatients = Sets.newTreeSet(Sets.newHashSet("Automobile"));
        Set<String> actualPatients = Sentence.getRoleEntities(CaseRole.PATIENT, roleScratchPad);

        assertEquals(expectedPatients, actualPatients);

        // Test toString().
        String expected = "agent Driving Human\n" +
                "agent Driving Tom\n" +
                "patient Driving Automobile\n";
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

        String expected = "agent Driving Human\n" +
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

    /**
     * Throws IllegalArgumentException because the events of the two processes don't match.
     */
    @Test
    public void testMergeMultipleRolesFail() {
        SumoProcessCollector process1 = new SumoProcessCollector(knowledgeBase, "agent", "Driving", "?H");

        SumoProcessCollector process2 = new SumoProcessCollector(knowledgeBase, "agent", "Eating", "?H");
        process2.addRole("patient", "?C");

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("Cannot merge because the objects do not have identical processes: process1 = Driving; process2 = Eating");
        process1.merge(process2);
    }

    @Test
    public void testMergeMultipleRolesNoIntersection() {
        SumoProcessCollector process1 = new SumoProcessCollector(knowledgeBase, "agent", "Driving", "?H");
        process1.addRole("patient", "?C");

        SumoProcessCollector process2 = new SumoProcessCollector(knowledgeBase, "patient", "Driving", "?D");
        process2.addRole("destination", "?P");

        process1.merge(process2);

        String expected = "agent Driving ?H\n" +
                "destination Driving ?P\n" +
                "patient Driving ?C\n" +
                "patient Driving ?D\n";
        assertEquals(expected, process1.toString());
        expected = "?H drives ?C and ?D to ?P";
        assertEquals(expected, process1.toNaturalLanguage());
    }

    @Test
    public void testMergeMultipleRolesAgentIntersection() {
        SumoProcessCollector process1 = new SumoProcessCollector(knowledgeBase, "agent", "Driving", "?H");
        process1.addRole("patient", "?C");

        SumoProcessCollector process2 = new SumoProcessCollector(knowledgeBase, "agent", "Driving", "?H");
        process2.addRole("patient", "?D");
        process2.addRole("destination", "?P");

        process1.merge(process2);

        String expected = "agent Driving ?H\n" +
                "destination Driving ?P\n" +
                "patient Driving ?C\n" +
                "patient Driving ?D\n";
        assertEquals(expected, process1.toString());
        expected = "?H drives ?C and ?D to ?P";
        assertEquals(expected, process1.toNaturalLanguage());
    }

    @Test
    public void testMergeMultipleRolesAgentIntersectionNegative() {
        SumoProcessCollector process1 = new SumoProcessCollector(knowledgeBase, "agent", "Driving", "?H");
        process1.addRole("patient", "?C");

        SumoProcessCollector process2 = new SumoProcessCollector(knowledgeBase, "agent", "Driving", "?H");
        process2.addRole("patient", "?D");
        process2.addRole("destination", "?P");
        process2.setPolarity(VerbProperties.Polarity.NEGATIVE);

        // Verify polarity before the merge.
        assertEquals(VerbProperties.Polarity.AFFIRMATIVE, process1.getPolarity());
        assertEquals(VerbProperties.Polarity.NEGATIVE, process2.getPolarity());

        process1.merge(process2);

        assertEquals(VerbProperties.Polarity.NEGATIVE, process1.getPolarity());

        String expected = "agent Driving ?H\n" +
                "destination Driving ?P\n" +
                "patient Driving ?C\n" +
                "patient Driving ?D\n";
        assertEquals(expected, process1.toString());
        expected = "?H doesn't drive ?C and ?D to ?P";
        assertEquals(expected, process1.toNaturalLanguage());
    }

}