package com.articulate.sigma;

import org.junit.Test;

import com.articulate.sigma.Diagnostics;
import com.articulate.sigma.Formula;
import com.articulate.sigma.IntegrationTestBase;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DiagnosticsTest extends IntegrationTestBase {
    @Test
    public void singlePredicateTest() {
        Formula f = new Formula("(agent ?A ?B)");
        Map<String, HashSet<String>> links = Diagnostics.findOrphanVars(f);

        assertTrue(links.containsKey("?A"));
        assertTrue(links.get("?A").contains("?B"));

        assertTrue(links.containsKey("?B"));
        assertTrue(links.get("?B").contains("?A"));

        // no self links
        assertFalse(links.get("?A").contains("?A"));
        assertFalse(links.get("?B").contains("?B"));
    }

    @Test
    public void multiplePredicateTest() {
        Formula f = new Formula("(and (agent ?A ?B) (patient ?B ?C) (location ?C ?D))");
        Map<String, HashSet<String>> links = Diagnostics.findOrphanVars(f);

        assertTrue(links.containsKey("?A"));
        assertTrue(links.get("?A").contains("?B"));

        assertTrue(links.containsKey("?B"));
        assertTrue(links.get("?B").contains("?A"));
        assertTrue(links.get("?B").contains("?C")); // from (patient ?B ?C)

        assertTrue(links.containsKey("?C"));
        assertTrue(links.get("?C").contains("?B"));
        assertTrue(links.get("?C").contains("?D")); // from (location ?C ?D)

        assertTrue(links.containsKey("?D"));
        assertTrue(links.get("?D").contains("?C"));

        // no self links
        assertFalse(links.get("?A").contains("?A"));
        assertFalse(links.get("?B").contains("?B"));
        assertFalse(links.get("?C").contains("?C"));
        assertFalse(links.get("?D").contains("?D"));
    }

    @Test 
    public void skipVarListTest() {
        Formula f = new Formula("(exists (?A) (and (agent ?X ?Y)))");
        Map<String, HashSet<String>> links = Diagnostics.findOrphanVars(f);

        // ?A should NOT appear
        assertFalse(links.containsKey("?A"));

        assertTrue(links.containsKey("?X"));
        assertTrue(links.get("?X").contains("?Y"));
        assertTrue(links.containsKey("?Y"));
        assertTrue(links.get("?Y").contains("?X"));
    }

    @Test
    public void existsVarListInBodyTest() {
        Formula f = new Formula("(exists (?A) (and (agent ?A ?B) (patient ?B ?C)))");
        Map<String, HashSet<String>> links = Diagnostics.findOrphanVars(f);

        // because ?A appears in the body, it should be present and linked
        assertTrue(links.containsKey("?A"));
        assertTrue(links.get("?A").contains("?B"));

        assertTrue(links.containsKey("?B"));
        assertTrue(links.get("?B").contains("?A"));
        assertTrue(links.get("?B").contains("?C"));

        assertTrue(links.containsKey("?C"));
        assertTrue(links.get("?C").contains("?B"));

        // no self links
        assertFalse(links.get("?A").contains("?A"));
        assertFalse(links.get("?B").contains("?B"));
        assertFalse(links.get("?C").contains("?C"));
    }
}