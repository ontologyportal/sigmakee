// package com.articulate.sigma;

// import org.junit.Test;
// import com.articulate.sigma.Diagnostics;
// import com.articulate.sigma.Formula;
// import com.articulate.sigma.IntegrationTestBase;
// import com.articulate.sigma.KIF;

// import static org.junit.Assert.*;
// import static org.junit.Assert.assertTrue;
// import com.google.common.collect.Sets;
// import java.util.Set;
// import java.util.Map;
// import java.beans.Transient;
// import java.io.File;
// import java.util.HashMap;
// import java.util.HashSet;


// public class DiagnosticsTest extends IntegrationTestBase {
//     @Test
//     public void testSimpleClause() {
//         Formula f = new Formula("(instance ?H Human)");
//         Map<String, Set<String>> links = Diagnostics.getVariableLinks(f, kb);
//         assertTrue(links.containsKey("?H"));
//         // checks only one variable is in the map
//         assertEquals(links.size() == 1, true);
//         // map should only have one key
//         assertTrue(links.get("?H").isEmpty());
//     }

//     @Test
//     public void testComplexClause() {
//         Formula f = new Formula("(=>\n" +
//                                 "  (and\n" +
//                                 "    (P ?A ?B)\n" +
//                                 "    (P ?B ?C))\n" +
//                                 "  (exists (?X ?Z)\n" +
//                                 "    (and\n" +
//                                 "      (Q ?X)\n" +
//                                 "      (M ?Z ?C))))"
//                                 );
//         Map<String, Set<String>> links = Diagnostics.getVariableLinks(f, kb);
//         // expected variables are ?A, ?B, ?C, ?X, ?Z
//         assertTrue(links.containsKey("?A"));
//         assertTrue(links.containsKey("?B"));
//         assertTrue(links.containsKey("?C"));
//         assertTrue(links.containsKey("?X"));
//         assertTrue(links.containsKey("?Z"));
//         // check if the map has exactly 5 keys
//         assertEquals(links.size() == 5, true);
//         // co-occurrence links from (P ?A ?B) and (P ?B ?C)
//         assertEquals(Set.of("?B"), links.get("?A"));
//         assertEquals(Set.of("?A", "?C"), links.get("?B"));
//         // ?C only co-occurs with ?B in the first part of the formula
//         assertTrue(links.get("?C").contains("?B"));
        
//         // ?X only occurs in (Q ?X), empty set
//         assertTrue(links.get("?X").isEmpty());

//         // co-occurrence links from (M ?Z ?C) : Z <-> C
//         assertTrue(links.get("?Z").contains("?C"));
//         assertTrue(links.get("?C").contains("?Z"));
//     }

//     @Test
//     public void testRealFormula() {
//         Formula complexFormula = new Formula("(=>\n" +
//                                                   "  (instance ?WARGAMING Wargaming)\n" +
//                                                   "  (exists (?MILITARYOFFICER ?SIMULATION ?TOOL)\n" +
//                                                   "    (and\n" +
//                                                   "      (instance ?MILITARYOFFICER MilitaryOfficer)\n" +
//                                                   "      (instance ?SIMULATION Imagining)\n" +
//                                                   "      (instance ?TOOL Device)\n" +
//                                                   "      (agent ?WARGAMING ?MILITARYOFFICER)\n" +
//                                                   "      (patient ?WARGAMING ?SIMULATION)\n" +
//                                                   "      (instrument ?WARGAMING ?TOOL))))"
//                                                );
//         Map<String, Set<String>> links = Diagnostics.getVariableLinks(complexFormula, kb);
       
//         // expected variables are ?WARGAMING, ?MILITARYOFFICER, ?SIMULATION, ?TOOL
//         assertTrue(links.containsKey("?WARGAMING"));
//         assertTrue(links.containsKey("?MILITARYOFFICER"));
//         assertTrue(links.containsKey("?SIMULATION"));
//         assertTrue(links.containsKey("?TOOL"));
//         assertEquals(links.size() == 4, true);

//         // ?WARGAMING co-occurs with ?MILITARYOFFICER, ?SIMULATION, and ?TOOL
//         assertEquals(Set.of("?MILITARYOFFICER", "?SIMULATION", "?TOOL"), links.get("?WARGAMING"));

//         // each of the other three variables only co-occur with ?WARGAMING
//         assertEquals(Set.of("?WARGAMING"), links.get("?MILITARYOFFICER"));
//         assertEquals(Set.of("?WARGAMING"), links.get("?SIMULATION"));
//         assertEquals(Set.of("?WARGAMING"), links.get("?TOOL"));
//     }

//     @Test
//     public void testOverloadedGetVariableLinks() {    
//         // test for Wargaming formula that is in test.kif
//         // File wargamingFile = new File(System.getenv("SIGMA_SRC") + "/test/integration/java/resources/test.kif");
//         // assertTrue(wargamingFile.exists()); 
//         // Map<String, Set<String>> links = Diagnostics.getVariableLinks(wargamingFile, kb);
//         // assertTrue(links.containsKey("?WARGAMING"));
//         // assertTrue(links.containsKey("?MILITARYOFFICER"));
//         // assertTrue(links.containsKey("?SIMULATION"));
//         // assertTrue(links.containsKey("?TOOL"));

//         // assertEquals(Set.of("?MILITARYOFFICER", "?SIMULATION", "?TOOL"), links.get("?WARGAMING"));
        
//         // assertEquals(Set.of("?WARGAMING"), links.get("?MILITARYOFFICER"));
//         // assertEquals(Set.of("?WARGAMING"), links.get("?SIMULATION"));
//         // assertEquals(Set.of("?WARGAMING"), links.get("?TOOL"));
        
//         // test for ServiceAnimal formula that is in test2.kif
//         // File servAnimalFile = new File(System.getenv("SIGMA_SRC") + "/test/integration/java/resources/test2.kif");
//         // assertTrue(servAnimalFile.exists()); 

//         // assertTrue(links.containsKey("?SA"));
//         // assertTrue(links.containsKey("?H"));
//         // assertTrue(links.containsKey("?HELP"));
//         // assertTrue(links.containsKey("?T"));
//         // assertTrue(links.containsKey("?REST"));
//         // assertTrue(links.containsKey("?P"));
//         // assertTrue(links.containsKey("?E"));

//         // // from (agent ?T ?SA), (agent ?HELP ?SA), (patient ?HELP ?H)
//         // assertEquals(Set.of("?T","?HELP","?E","?REST","?P"), links.get("?SA"));
//         // assertEquals(Set.of("?SA"), links.get("?T"));
//         // assertEquals(Set.of("?SA","?H"), links.get("?HELP"));
//         // assertEquals(Set.of("?HELP"), links.get("?H"));

//         // // from (path ?E ?SA), (located ?REST ?SA), (located ?P ?SA)
//         // assertEquals(Set.of("?SA"), links.get("?E"));
//         // assertEquals(Set.of("?SA"), links.get("?REST"));
//         // assertEquals(Set.of("?SA"), links.get("?P"));
//     }
// }
