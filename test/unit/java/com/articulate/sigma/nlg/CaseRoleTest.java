package com.articulate.sigma.nlg;

import com.articulate.sigma.UnitTestBase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

// Tests requiring that KBs be loaded.

public class CaseRoleTest extends UnitTestBase {

    @Test
    public void testCommonCaseRoles() {
        CaseRole caseRole = CaseRole.toCaseRole("agent", kb);
        assertEquals(CaseRole.AGENT, caseRole);

        caseRole = CaseRole.toCaseRole("attends", kb);
        assertEquals(CaseRole.ATTENDS, caseRole);

        caseRole = CaseRole.toCaseRole("destination", kb);
        assertEquals(CaseRole.DESTINATION, caseRole);

        caseRole = CaseRole.toCaseRole("direction", kb);
        assertEquals(CaseRole.DIRECTION, caseRole);

        caseRole = CaseRole.toCaseRole("eventPartlyLocated", kb);
        assertEquals(CaseRole.EVENTPARTLYLOCATED, caseRole);

        caseRole = CaseRole.toCaseRole("experiencer", kb);
        assertEquals(CaseRole.EXPERIENCER, caseRole);

        caseRole = CaseRole.toCaseRole("instrument", kb);
        assertEquals(CaseRole.INSTRUMENT, caseRole);

        caseRole = CaseRole.toCaseRole("moves", kb);
        assertEquals(CaseRole.MOVES, caseRole);

        caseRole = CaseRole.toCaseRole("origin", kb);
        assertEquals(CaseRole.ORIGIN, caseRole);

        caseRole = CaseRole.toCaseRole("patient", kb);
        assertEquals(CaseRole.PATIENT, caseRole);

        caseRole = CaseRole.toCaseRole("path", kb);
        assertEquals(CaseRole.PATH, caseRole);

        caseRole = CaseRole.toCaseRole("resource", kb);
        assertEquals(CaseRole.RESOURCE, caseRole);

        caseRole = CaseRole.toCaseRole("other", kb);
        assertEquals(CaseRole.OTHER, caseRole);

        caseRole = CaseRole.toCaseRole("blahblah", kb);
        assertEquals(CaseRole.OTHER, caseRole);
    }

    /**
     * Test less common case roles that are subrelations of a common case role.
     */
    @Test
    public void testSubrelationCaseRoles() {
        CaseRole caseRole = CaseRole.toCaseRole("result", kb);
        assertEquals(CaseRole.PATIENT, caseRole);

        caseRole = CaseRole.toCaseRole("eventLocated", kb);
        assertEquals(CaseRole.EVENTPARTLYLOCATED, caseRole);

        caseRole = CaseRole.toCaseRole("changesLocation", kb);
        assertEquals(CaseRole.MOVES, caseRole);

        // partly located is not a case role
        caseRole = CaseRole.toCaseRole("partlyLocated", kb);
        assertEquals(CaseRole.OTHER, caseRole);
    }

}