package com.articulate.sigma;

import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;

public class CaseRoleTest extends IntegrationTestBase {

    /**
     * This test is meant to detect errors in the writing of SUMO rules in kif files. It fails if it finds a case where a term
     * is declared to be a subrelation of another term that is a CaseRole, but the first term is not explicitly declared to be an
     * instance of CaseRole. For example, "(subrelation standardInputDevice instrument)" by itself will fail because instrument
     * is a CaseRole, but we haven't explicitly said standardInputDevice is a CaseRole. The test will pass if the kif file reads
     * "(subrelation standardInputDevice instrument) /n (instance standardInputDevice CaseRole)".
     */
    @Test
    public void testCaseRole() {

        KBcache cache = SigmaTestBase.kb.kbCache;

        // Collect all expected instances for "CaseRole", by running KBcache.buildTransInstOf()
        cache.instanceOf = new HashMap<>();
        cache.buildTransInstOf();
        HashMap<String, HashSet<String>> expectedInstancesMap = cache.instanceOf;
        TreeSet<String> expectedInstancesForCaseRole = new TreeSet<>();
        for (String inst : expectedInstancesMap.keySet()) {
            HashSet<String> parentClasses = expectedInstancesMap.get(inst);
            if (parentClasses.contains("CaseRole"))
                expectedInstancesForCaseRole.add(inst);
        }

        // Collect all actual instances for "CaseRole", by running KBcache.buildDirectInstances()
        cache.instanceOf = new HashMap<>();
        cache.buildDirectInstances();
        HashMap<String, HashSet<String>> actualInstancesMap = cache.instanceOf;
        TreeSet<String> actualInstancesForCaseRole = new TreeSet<>();
        for (String inst : actualInstancesMap.keySet()) {
            HashSet<String> parentClasses = actualInstancesMap.get(inst);
            if (parentClasses.contains("CaseRole"))
                actualInstancesForCaseRole.add(inst);
        }

        assertEquals(expectedInstancesForCaseRole, actualInstancesForCaseRole);
    }
}
