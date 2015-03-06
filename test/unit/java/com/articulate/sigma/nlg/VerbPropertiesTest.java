package com.articulate.sigma.nlg;

import com.google.common.collect.Lists;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class VerbPropertiesTest {

    private final VerbPropertiesSimpleImpl verbPropertiesSimple = new VerbPropertiesSimpleImpl();

    @Test
    public void testPrepositionDefault() {
        String inputVerb = "blahblah";

        List<String> expected = Lists.newArrayList("");
        List<String> actual  = verbPropertiesSimple.getPrepositionForCaseRole(inputVerb, CaseRole.AGENT);
        assertEquals(expected, actual);

        expected = Lists.newArrayList("to");
        actual  = verbPropertiesSimple.getPrepositionForCaseRole(inputVerb, CaseRole.DESTINATION);
        assertEquals(expected, actual);

        expected = Lists.newArrayList("toward");
        actual  = verbPropertiesSimple.getPrepositionForCaseRole(inputVerb, CaseRole.DIRECTION);
        assertEquals(expected, actual);

        expected = Lists.newArrayList("in");
        actual  = verbPropertiesSimple.getPrepositionForCaseRole(inputVerb, CaseRole.EVENTPARTLYLOCATED);
        assertEquals(expected, actual);

        expected = Lists.newArrayList("");
        actual  = verbPropertiesSimple.getPrepositionForCaseRole(inputVerb, CaseRole.EXPERIENCER);
        assertEquals(expected, actual);

        expected = Lists.newArrayList("with");
        actual  = verbPropertiesSimple.getPrepositionForCaseRole(inputVerb, CaseRole.INSTRUMENT);
        assertEquals(expected, actual);

        expected = Lists.newArrayList("");
        actual  = verbPropertiesSimple.getPrepositionForCaseRole(inputVerb, CaseRole.MOVES);
        assertEquals(expected, actual);

        expected = Lists.newArrayList("from");
        actual  = verbPropertiesSimple.getPrepositionForCaseRole(inputVerb, CaseRole.ORIGIN);
        assertEquals(expected, actual);

        expected = Lists.newArrayList("along");
        actual  = verbPropertiesSimple.getPrepositionForCaseRole(inputVerb, CaseRole.PATH);
        assertEquals(expected, actual);

        expected = Lists.newArrayList("");
        actual  = verbPropertiesSimple.getPrepositionForCaseRole(inputVerb, CaseRole.PATIENT);
        assertEquals(expected, actual);

        expected = Lists.newArrayList("out of", "from");
        actual  = verbPropertiesSimple.getPrepositionForCaseRole(inputVerb, CaseRole.RESOURCE);
        assertEquals(expected, actual);

        expected = Lists.newArrayList("");
        actual  = verbPropertiesSimple.getPrepositionForCaseRole(inputVerb, CaseRole.OTHER);
        assertEquals(expected, actual);
    }

    @Test
    public void testCaseRoleDefault() {
        String inputVerb = "blahblah";

        List<CaseRole> expected = Lists.newArrayList(CaseRole.AGENT, CaseRole.EXPERIENCER, CaseRole.MOVES);
        List<CaseRole> actual = verbPropertiesSimple.getCaseRolesForGrammarRole(inputVerb, SVOElement.SVOGrammarPosition.SUBJECT);
        assertEquals(expected, actual);

        expected = Lists.newArrayList(CaseRole.PATIENT, CaseRole.MOVES);
        actual = verbPropertiesSimple.getCaseRolesForGrammarRole(inputVerb, SVOElement.SVOGrammarPosition.DIRECT_OBJECT);
        assertEquals(expected, actual);

        expected = Lists.newArrayList(CaseRole.DIRECTION, CaseRole.PATH, CaseRole.ORIGIN, CaseRole.DESTINATION,
                CaseRole.EVENTPARTLYLOCATED, CaseRole.INSTRUMENT, CaseRole.RESOURCE);
        actual = verbPropertiesSimple.getCaseRolesForGrammarRole(inputVerb, SVOElement.SVOGrammarPosition.INDIRECT_OBJECT);
        assertEquals(expected, actual);
    }

    @Test
    public void testCaseRoleBurn() {
        String inputVerb = "burn";

        List<CaseRole> expected = Lists.newArrayList(CaseRole.AGENT, CaseRole.PATIENT);
        List<CaseRole> actual = verbPropertiesSimple.getCaseRolesForGrammarRole(inputVerb, SVOElement.SVOGrammarPosition.SUBJECT);
        assertEquals(expected, actual);

        // Falls back to default values for direct object.
        expected = Lists.newArrayList(CaseRole.PATIENT, CaseRole.MOVES);
        actual = verbPropertiesSimple.getCaseRolesForGrammarRole(inputVerb, SVOElement.SVOGrammarPosition.DIRECT_OBJECT);
        assertEquals(expected, actual);
    }

    @Test
    public void testCaseRoleFall() {
        String inputVerb = "fall";

        List<CaseRole> expected = Lists.newArrayList(CaseRole.EXPERIENCER, CaseRole.PATIENT);
        List<CaseRole> actual = verbPropertiesSimple.getCaseRolesForGrammarRole(inputVerb, SVOElement.SVOGrammarPosition.SUBJECT);
        assertEquals(expected, actual);
    }

    @Test
    public void testCaseRoleSee() {
        String inputVerb = "see";

        List<CaseRole> expected = Lists.newArrayList(CaseRole.EXPERIENCER);
        List<CaseRole> actual = verbPropertiesSimple.getCaseRolesForGrammarRole(inputVerb, SVOElement.SVOGrammarPosition.SUBJECT);
        assertEquals(expected, actual);
    }
}
