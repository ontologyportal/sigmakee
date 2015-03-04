package com.articulate.sigma.nlg;

import com.articulate.sigma.UnitTestBase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

// Tests requiring that KBs be loaded.

public class CaseRoleTest extends UnitTestBase {

    @Test
    public void testCommonCaseRoles() {
        CaseRole caseRole = CaseRole.toCaseRole("agent");
        assertEquals(CaseRole.AGENT, caseRole);

        caseRole = CaseRole.toCaseRole("destination");
        assertEquals(CaseRole.DESTINATION, caseRole);

        caseRole = CaseRole.toCaseRole("direction");
        assertEquals(CaseRole.DIRECTION, caseRole);

        caseRole = CaseRole.toCaseRole("eventPartlyLocated");
        assertEquals(CaseRole.EVENTPARTLYLOCATED, caseRole);

        caseRole = CaseRole.toCaseRole("experiencer");
        assertEquals(CaseRole.EXPERIENCER, caseRole);

        caseRole = CaseRole.toCaseRole("instrument");
        assertEquals(CaseRole.INSTRUMENT, caseRole);

        caseRole = CaseRole.toCaseRole("moves");
        assertEquals(CaseRole.MOVES, caseRole);

        caseRole = CaseRole.toCaseRole("origin");
        assertEquals(CaseRole.ORIGIN, caseRole);

        caseRole = CaseRole.toCaseRole("patient");
        assertEquals(CaseRole.PATIENT, caseRole);

        caseRole = CaseRole.toCaseRole("path");
        assertEquals(CaseRole.PATH, caseRole);

        caseRole = CaseRole.toCaseRole("resource");
        assertEquals(CaseRole.RESOURCE, caseRole);

        caseRole = CaseRole.toCaseRole("other");
        assertEquals(CaseRole.OTHER, caseRole);

        caseRole = CaseRole.toCaseRole("blahblahblah");
        assertEquals(CaseRole.OTHER, caseRole);
    }

    /**
     * Test less common case roles that are subrelations of a common case role.
     */
    @Test
    public void testSubrelationCaseRoles() {
        CaseRole caseRole = CaseRole.toCaseRole("result");
        assertEquals(CaseRole.PATIENT, caseRole);

        caseRole = CaseRole.toCaseRole("eventLocated");
        assertEquals(CaseRole.EVENTPARTLYLOCATED, caseRole);

        caseRole = CaseRole.toCaseRole("changesLocation");
        assertEquals(CaseRole.MOVES, caseRole);

        caseRole = CaseRole.toCaseRole("attends");
        assertEquals(CaseRole.EXPERIENCER, caseRole);

        // partly located is not a case role
        caseRole = CaseRole.toCaseRole("partlyLocated");
        assertEquals(CaseRole.OTHER, caseRole);
    }

}