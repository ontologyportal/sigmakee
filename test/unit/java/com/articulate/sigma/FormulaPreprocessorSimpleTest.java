package com.articulate.sigma;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;


// Tests on FormulaPreprocessor that do not require KBs be loaded. Note kbMock in base class.
public class FormulaPreprocessorSimpleTest extends SigmaMockTestBase {

    @Test
    public void testGetCaseRoles() {
        Map<String, HashSet<String>> varMap = Maps.newHashMap();
        varMap.put("?D", Sets.newHashSet("Driving"));
        varMap.put("?H", Sets.newHashSet("Human"));

        Multimap<String, String> caseRoleMap = HashMultimap.create();
        caseRoleMap.put("agent", "?D ?H");

        FormulaPreprocessor fp = new FormulaPreprocessor();
        List<SumoProcess> actual = fp.getCaseRoles(varMap, caseRoleMap, kbMock);

        SumoProcess expected = new SumoProcess(kbMock, "agent", "Driving Human");

        assertEquals(1, actual.size());
        assertEquals(expected.toString(), actual.get(0).toString());
    }

}