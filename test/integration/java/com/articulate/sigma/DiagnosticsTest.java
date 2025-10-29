package com.articulate.sigma;

import org.junit.Test;

import com.articulate.sigma.Diagnostics;
import com.articulate.sigma.Formula;
import com.articulate.sigma.IntegrationTestBase;
import com.articulate.sigma.KIF;

import java.beans.Transient;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DiagnosticsTest extends IntegrationTestBase {
    @Test
    public void orphanVariableTest() {
        // two disconnected variable groups: {?X, ?Y} and {?A, ?B}
        Formula f = new Formula("(and (agent ?X ?Y) (patient ?A ?B))");
        Map<String, HashSet<String>> links = Diagnostics.findOrphanVars(f);

        // ?X and ?Y should be connected
        assertTrue(links.containsKey("?X"));
        assertTrue(links.get("?X").contains("?Y"));
        assertTrue(links.containsKey("?Y"));
        assertTrue(links.get("?Y").contains("?X"));

        // ?A and ?B should be connected
        assertTrue(links.containsKey("?A"));
        assertTrue(links.get("?A").contains("?B"));
        assertTrue(links.containsKey("?B"));
        assertTrue(links.get("?B").contains("?A"));

        // But the two groups should NOT be connected
        // This is the orphan/disconnected variable set scenario
        assertFalse(links.get("?X").contains("?A"));
        assertFalse(links.get("?X").contains("?B"));
        assertFalse(links.get("?Y").contains("?A"));
        assertFalse(links.get("?Y").contains("?B"));
        assertFalse(links.get("?A").contains("?X"));
        assertFalse(links.get("?A").contains("?Y"));
        assertFalse(links.get("?B").contains("?X"));
        assertFalse(links.get("?B").contains("?Y"));
    }

    @Test
    public void singleOrphanVariableTest() {
        // ?Z appears only in quantifier list but not in body
        Formula f = new Formula("(exists (?Z) (agent ?X ?Y))");
        Map<String, HashSet<String>> links = Diagnostics.findOrphanVars(f);

        // ?Z should not appear at all (not even as a key)
        assertFalse(links.containsKey("?Z"));

        // ?X and ?Y should still be connected
        assertTrue(links.containsKey("?X"));
        assertTrue(links.get("?X").contains("?Y"));
        assertTrue(links.containsKey("?Y"));
        assertTrue(links.get("?Y").contains("?X"));
    }

    @Test
    public void nestedQuantifiersTest() {
        // Multiple nested quantifiers
        Formula f = new Formula(
            "(exists (?A) " +
            "  (and " +
            "    (agent ?A ?B) " +
            "    (exists (?C) " +
            "      (patient ?B ?C))))"
        );
        Map<String, HashSet<String>> links = Diagnostics.findOrphanVars(f);

        assertTrue(links.containsKey("?A"));
        assertTrue(links.get("?A").contains("?B"));

        assertTrue(links.containsKey("?B"));
        assertTrue(links.get("?B").contains("?A"));
        assertTrue(links.get("?B").contains("?C"));

        assertTrue(links.containsKey("?C"));
        assertTrue(links.get("?C").contains("?B"));

        // No self-links
        assertFalse(links.get("?A").contains("?A"));
        assertFalse(links.get("?B").contains("?B"));
        assertFalse(links.get("?C").contains("?C"));
    }

    @Test
    public void noVariablesTest() {
        Formula f = new Formula("(instance Suji Human)");
        Map<String, HashSet<String>> links = Diagnostics.findOrphanVars(f);

        // Should return empty map
        assertTrue(links.isEmpty());
    }

    @Test
    public void singleVariableTest() {
        Formula f = new Formula("(instance ?X Human)");
        Map<String, HashSet<String>> links = Diagnostics.findOrphanVars(f);

        // ?X should exist but have no neighbors (it's alone)
        assertTrue(links.containsKey("?X"));
        assertTrue(links.get("?X").isEmpty());
    }

    @Test
    public void forallQuantifierTest() {
        Formula f = new Formula(
            "(forall (?X) " +
            "  (=> " +
            "    (instance ?X Human) " +
            "    (exists (?Y) " +
            "      (parent ?Y ?X))))"
        );
        Map<String, HashSet<String>> links = Diagnostics.findOrphanVars(f);

        // ?X and ?Y should be linked through the parent predicate
        assertTrue(links.containsKey("?X"));
        assertTrue(links.get("?X").contains("?Y"));
        assertTrue(links.containsKey("?Y"));
        assertTrue(links.get("?Y").contains("?X"));
    }

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

    @Test
    public void equalTest() {
        Formula f = new Formula(
            "(=>\n" +
            "  (and\n" +
            "    (instance ?LIST ConsecutiveTimeIntervalList)\n" +
            "    (equal ?T1 (ListOrderFn ?LIST ?N))\n" +
            "    (equal ?T2 (ListOrderFn ?LIST (AdditionFn ?N 1))))\n" +
            "  (equal (BeginFn ?T2) (EndFn ?T1)))"
        );
        Map<String, HashSet<String>> links = Diagnostics.findOrphanVars(f);
        // ?T1: ?LIST ?N ?T2
        assertTrue(links.containsKey("?T1"));
        assertTrue(links.get("?T1").contains("?LIST"));
        assertTrue(links.get("?T1").contains("?N"));
        assertTrue(links.get("?T1").contains("?T2"));

        // ?T2: ?LIST ?N ?T1
        assertTrue(links.containsKey("?T2"));
        assertTrue(links.get("?T2").contains("?LIST"));
        assertTrue(links.get("?T2").contains("?N"));
        assertTrue(links.get("?T2").contains("?T1"));

        // ?LIST: ?T1 ?T2 ?N
        assertTrue(links.containsKey("?LIST"));
        assertTrue(links.get("?LIST").contains("?T1"));
        assertTrue(links.get("?LIST").contains("?T2"));
        assertTrue(links.get("?LIST").contains("?N"));

        // ?N: ?LIST ?T1 ?T2
        assertTrue(links.containsKey("?N"));
        assertTrue(links.get("?N").contains("?LIST"));
        assertTrue(links.get("?N").contains("?T1"));
        assertTrue(links.get("?N").contains("?T2"));

        // no self links
        assertFalse(links.get("?T1").contains("?T1"));
        assertFalse(links.get("?T2").contains("?T2"));
        assertFalse(links.get("?LIST").contains("?LIST"));
        assertFalse(links.get("?N").contains("?N"));
    }

    @Test 
    public void HOLTest() {
        Formula f = new Formula(
            "(<=>\n" +
            "  (and\n" +
            "    (instance ?B BodyPart)\n" +
            "    (holdsDuring ?T (attribute ?B Bare)))\n" +
            "  (holdsDuring ?T (not (exists (?C)\n" +
            "    (and (instance ?C Clothing) (covers ?C ?B))))))"
        );
        Map<String, HashSet<String>> links = Diagnostics.findOrphanVars(f);
        // ?T: ?C ?B
        assertTrue(links.containsKey("?T"));
        assertTrue(links.get("?T").contains("?C"));
        assertTrue(links.get("?T").contains("?B"));

        // ?C: ?T ?B
        assertTrue(links.containsKey("?C"));
        assertTrue(links.get("?C").contains("?T"));
        assertTrue(links.get("?C").contains("?B"));

        // ?B: ?T ?C
        assertTrue(links.containsKey("?B"));
        assertTrue(links.get("?B").contains("?T"));
        assertTrue(links.get("?B").contains("?C"));

        // no self links
        assertFalse(links.get("?T").contains("?T"));
        assertFalse(links.get("?C").contains("?C"));
        assertFalse(links.get("?B").contains("?B"));
    }

    @Test 
    public void networkHackingFormulaTest() {
        String pt1 =
            "(=>\n" +
            "  (and\n" +
            "    (instance ?CN ComputerNetwork)\n" +
            "    (attribute ?CN VulnerableNetwork))\n" +
            "  (exists (?NetworkOwner)\n" +
            "    (and\n" +
            "      (instance ?NetworkOwner Human)\n" +
            "      (modalAttribute\n" +
            "        (exists (?Person ?NL)\n" +
            "          (and\n" +
            "            (instance ?Person Human)\n" +
            "            (instance ?NL NetworkLogin)\n" +
            "            (agent ?NL ?Person)\n" +
            "            (patient ?CN ?Person) Possibility)))\n" +
            "      (not\n" +
            "        (desires ?NetworkOwner\n" +
            "          (exists (?Person ?NL)\n" +
            "            (and\n" +
            "              (instance ?Person Human)\n" +
            "              (instance ?NL NetworkLogin)\n" +
            "              (agent ?NL ?Person)\n" +
            "              (patient ?CN ?Person))))))))";

        String pt2 =
            "(=>\n" +
            "  (instance ?NL NetworkLogin)\n" +
            "  (exists (?CN)\n" +
            "    (and\n" +
            "      (instance ?CN ComputerNetwork)\n" +
            "      (instrument ?NL ?CN))))";

        String pt3 =
            "(=>\n" +
            "  (and\n" +
            "    (instance ?NL NetworkLogin)\n" +
            "    (instance ?CN ComputerNetwork)\n" +
            "    (instance ?User Human)\n" +
            "    (instance ?Computer Computer)\n" +
            "    (agent ?NL ?User)\n" +
            "    (patient ?NL ?CN)\n" +
            "    (part ?Computer ?CN))\n" +
            "  (modalAttribute\n" +
            "    (exists (?LogIn)\n" +
            "      (and\n" +
            "        (instance ?LogIn LoggingIn)\n" +
            "        (agent ?LogIn ?User)\n" +
            "        (patient ?LogIn ?Computer))) Possibility))";

        Formula f1 = new Formula(pt1);
        Formula f2 = new Formula(pt2);
        Formula f3 = new Formula(pt3);

        // Merge the three co-occurrence maps
        Map<String, HashSet<String>> links = new HashMap<>();
        Map<String, HashSet<String>> links1 = Diagnostics.findOrphanVars(f1);
        Map<String, HashSet<String>> links2 = Diagnostics.findOrphanVars(f2);
        Map<String, HashSet<String>> links3 = Diagnostics.findOrphanVars(f3);

        for (String var : links1.keySet()) {
            links.putIfAbsent(var, new HashSet<String>());
            links.get(var).addAll(links1.get(var));
        }
        for (String var : links2.keySet()) {
            links.putIfAbsent(var, new HashSet<String>());
            links.get(var).addAll(links2.get(var));
        }
        for (String var : links3.keySet()) {
            links.putIfAbsent(var, new HashSet<String>());
            links.get(var).addAll(links3.get(var));
        }

        assertTrue(links.containsKey("?NL"));
        assertTrue(links.get("?NL").contains("?User"));
        assertTrue(links.get("?NL").contains("?CN"));

        
        assertTrue(links.containsKey("?Computer"));
        assertTrue(links.get("?Computer").contains("?CN"));
        assertTrue(links.containsKey("?CN"));
        assertTrue(links.get("?CN").contains("?Computer"));

        assertTrue(links.containsKey("?LogIn"));
        assertTrue(links.get("?LogIn").contains("?User"));
        assertTrue(links.get("?LogIn").contains("?Computer"));

        assertTrue(links.containsKey("?Person"));
        assertTrue(links.get("?Person").contains("?NL"));
        assertTrue(links.get("?Person").contains("?CN"));

        assertTrue(links.containsKey("?NetworkOwner"));
        assertTrue(links.get("?NetworkOwner").contains("?Person"));
        assertTrue(links.get("?NetworkOwner").contains("?NL"));
        assertTrue(links.get("?NetworkOwner").contains("?CN"));

        assertFalse(links.containsKey("?NL") && links.get("?NL").contains("?NL"));
        assertFalse(links.containsKey("?CN") && links.get("?CN").contains("?CN"));
        assertFalse(links.containsKey("?User") && links.get("?User").contains("?User"));
        assertFalse(links.containsKey("?Person") && links.get("?Person").contains("?Person"));
    }

    @Test
    public void acknowledgingFormulaTest() {
        String rule1 =
            "(=>\n" +
            "  (and\n" +
            "    (instance ?ACK Acknowledging)\n" +
            "    (instance ?SENDER Human)\n" +
            "    (instance ?RECEIVER Human)\n" +
            "    (agent ?ACK ?RECEIVER)\n" +
            "    (patient ?ACK ?SENDER))\n" +
            "  (containsFormula ?ACK\n" +
            "    (exists (?COMMUNICATION ?SENDER ?RECEIVER ?HEARING)\n" +
            "      (and\n" +
            "        (instance ?COMMUNICATION Communication)\n" +
            "        (instance ?HEARING Hearing)\n" +
            "        (agent ?COMMUNICATION ?SENDER)\n" +
            "        (patient ?COMMUNICATION ?RECEIVER)\n" +
            "        (experiencer ?HEARING ?RECEIVER)\n" +
            "        (subProcess ?HEARING ?COMMUNICATION)))))";

        String rule2 =
            "(=>\n" +
            "  (and\n" +
            "    (instance ?ACK Acknowledging)\n" +
            "    (agent ?ACK ?RECEIVER)\n" +
            "    (patient ?ACK ?SENDER))\n" +
            "  (exists (?COMMUNICATION)\n" +
            "    (and\n" +
            "      (instance ?COMMUNICATION Communication)\n" +
            "      (agent ?COMMUNICATION ?SENDER)\n" +
            "      (patient ?COMMUNICATION ?RECEIVER)\n" +
            "      (earlier\n" +
            "        (WhenFn ?COMMUNICATION)\n" +
            "        (WhenFn ?ACK)))))";

        Formula f1 = new Formula(rule1);
        Formula f2 = new Formula(rule2);

        Map<String, HashSet<String>> links = new HashMap<>();
        Map<String, HashSet<String>> links1 = Diagnostics.findOrphanVars(f1);
        Map<String, HashSet<String>> links2 = Diagnostics.findOrphanVars(f2);

        for (String var : links1.keySet()) {
            links.putIfAbsent(var, new HashSet<String>());
            links.get(var).addAll(links1.get(var));
        }
        for (String var : links2.keySet()) {
            links.putIfAbsent(var, new HashSet<String>());
            links.get(var).addAll(links2.get(var));
        }

        // ?ACK: {?RECEIVER, ?SENDER, ?COMMUNICATION, ?HEARING}
        assertTrue(links.containsKey("?ACK"));
        assertTrue(links.get("?ACK").contains("?RECEIVER"));
        assertTrue(links.get("?ACK").contains("?SENDER"));
        assertTrue(links.get("?ACK").contains("?COMMUNICATION"));
        assertTrue(links.get("?ACK").contains("?HEARING"));

        // ?COMMUNICATION: {?SENDER, ?RECEIVER, ?HEARING, ?ACK}
        assertTrue(links.containsKey("?COMMUNICATION"));
        assertTrue(links.get("?COMMUNICATION").contains("?SENDER"));
        assertTrue(links.get("?COMMUNICATION").contains("?RECEIVER"));
        assertTrue(links.get("?COMMUNICATION").contains("?HEARING"));
        assertTrue(links.get("?COMMUNICATION").contains("?ACK"));

        // ?HEARING: {?RECEIVER, ?COMMUNICATION, ?ACK}
        assertTrue(links.containsKey("?HEARING"));
        assertTrue(links.get("?HEARING").contains("?RECEIVER"));
        assertTrue(links.get("?HEARING").contains("?COMMUNICATION"));
        assertTrue(links.get("?HEARING").contains("?ACK"));

        // ?SENDER: {?ACK, ?COMMUNICATION}
        assertTrue(links.containsKey("?SENDER"));
        assertTrue(links.get("?SENDER").contains("?ACK"));
        assertTrue(links.get("?SENDER").contains("?COMMUNICATION"));

        // ?RECEIVER: {?ACK, ?COMMUNICATION, ?HEARING}
        assertTrue(links.containsKey("?RECEIVER"));
        assertTrue(links.get("?RECEIVER").contains("?ACK"));
        assertTrue(links.get("?RECEIVER").contains("?COMMUNICATION"));
        assertTrue(links.get("?RECEIVER").contains("?HEARING"));

        // No self-links
        assertFalse(links.get("?ACK").contains("?ACK"));
        assertFalse(links.get("?COMMUNICATION").contains("?COMMUNICATION"));
        assertFalse(links.get("?HEARING").contains("?HEARING"));
        assertFalse(links.get("?SENDER").contains("?SENDER"));
        assertFalse(links.get("?RECEIVER").contains("?RECEIVER"));
    }

    @Test
    public void orphanSingleVariableTest() {
        Formula f = new Formula("(instance ?X Human)");
        Map<String, HashSet<String>> links = Diagnostics.findOrphanVars(f);

        // ?X exists but has no neighbors
        assertTrue(links.containsKey("?X"));
        assertTrue(links.get("?X").isEmpty()); 
    }

    @Test
    public void threeVariablesOneOrphanTest() {
        // ?X and ?Y are connected, but ?Z is alone
        Formula f = new Formula(
            "(=> " +
            "  (and " +
            "    (agent ?X ?Y) " +
            "    (instance ?Z Human)) " +
            "  (patient ?X ?Y))"
        );
        Map<String, HashSet<String>> links = Diagnostics.findOrphanVars(f);
        
        // ?X and ?Y are connected
        assertTrue(links.containsKey("?X"));
        assertTrue(links.get("?X").contains("?Y"));
        
        assertTrue(links.containsKey("?Y"));
        assertTrue(links.get("?Y").contains("?X"));
        
        // ?Z exists but is orphaned (no neighbors)
        assertTrue(links.containsKey("?Z"));
        assertTrue(links.get("?Z").isEmpty());
        
        // ?Z is not connected to ?X or ?Y
        assertFalse(links.get("?X").contains("?Z"));
        assertFalse(links.get("?Y").contains("?Z"));
    }

    @Test
    public void singleVariableRepeatedTest() {
        // single variable ?T appearing on both sides of implication
        // this should NOT be flagged as an orphan since it appears multiple times
        Formula f = new Formula(
            "(=> " +
            "  (holdsDuring ?T (attribute EarthsMoon FullMoon)) " +
            "  (holdsDuring ?T (moonLitPortion 1 FullMoon)))"
        );
        Map<String, HashSet<String>> links = Diagnostics.findOrphanVars(f);
        
        // ?T exists but has no neighbors (only variable in formula)
        assertTrue(links.containsKey("?T"));
        assertTrue(links.get("?T").isEmpty());
        
        // note: this should NOT trigger a warning because ?T appears twice
    }

    @Test
    public void variablesConnectedThroughFunctionsTest() {
        Formula f = new Formula(
            "(=> " +
            "  (and " +
            "    (equal (MeasureFn ?X HourDuration) (TimeToFailureFn ?D)) " +
            "    (attribute ?D NonRepairable) " +
            "    (deviceFailTime ?D ?F) " +
            "    (deviceUpTime ?D ?U) " +
            "    (earlier ?U ?F)) " +
            "  (duration " +
            "    (TimeIntervalFn (BeginFn ?U) (BeginFn ?F)) " +
            "    (MeasureFn ?X HourDuration)))"
        );
        Map<String, HashSet<String>> links = Diagnostics.findOrphanVars(f);
        
        // all variables should be fully connected
        // ?X is connected to ?D, ?U, ?F through functions
        assertTrue(links.containsKey("?X"));
        assertTrue(links.get("?X").contains("?D"));
        assertTrue(links.get("?X").contains("?U"));
        assertTrue(links.get("?X").contains("?F"));
        
        // ?D is connected to ?X, ?U, ?F
        assertTrue(links.containsKey("?D"));
        assertTrue(links.get("?D").contains("?X"));
        assertTrue(links.get("?D").contains("?U"));
        assertTrue(links.get("?D").contains("?F"));
        
        // ?U is connected to all others
        assertTrue(links.containsKey("?U"));
        assertTrue(links.get("?U").contains("?D"));
        assertTrue(links.get("?U").contains("?F"));
        assertTrue(links.get("?U").contains("?X"));
        
        // ?F is connected to all others
        assertTrue(links.containsKey("?F"));
        assertTrue(links.get("?F").contains("?D"));
        assertTrue(links.get("?F").contains("?U"));
        assertTrue(links.get("?F").contains("?X"));
        
        // no self-links
        assertFalse(links.get("?X").contains("?X"));
        assertFalse(links.get("?D").contains("?D"));
        assertFalse(links.get("?U").contains("?U"));
        assertFalse(links.get("?F").contains("?F"));
    }
}