package com.articulate.sigma;

import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;

public class CaseRoleTest extends IntegrationTestBase {

    @Test
    public void testCaseRole() {

        KBcache cache = SigmaTestBase.kb.kbCache;

        // Collect all actual instances for "CaseRole", by running KBcache.buildDirectInstances()
        cache.instances = new HashMap<>();
        cache.buildDirectInstances();
        HashMap<String, HashSet<String>> actualInstancesMap = cache.instances;
        TreeSet<String> actualInstancesForCaseRole = new TreeSet<>();
        for (String inst : actualInstancesMap.keySet()) {
            HashSet<String> parentClasses = actualInstancesMap.get(inst);
            if (parentClasses.contains("CaseRole"))
                actualInstancesForCaseRole.add(inst);
        }

        // Collect all expected instances for "CaseRole", by running KBcache.buildTransInstOf()
        cache.instances = new HashMap<>();
        cache.buildTransInstOf();
        HashMap<String, HashSet<String>> expectedInstancesMap = cache.instances;
        TreeSet<String> expectedInstancesForCaseRole = new TreeSet<>();
        for (String inst : expectedInstancesMap.keySet()) {
            HashSet<String> parentClasses = expectedInstancesMap.get(inst);
            if (parentClasses.contains("CaseRole"))
                expectedInstancesForCaseRole.add(inst);
        }

        assertEquals(expectedInstancesForCaseRole, actualInstancesForCaseRole);
    }
}
