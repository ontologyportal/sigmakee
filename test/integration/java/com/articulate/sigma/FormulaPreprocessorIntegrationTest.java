package com.articulate.sigma;

import com.articulate.sigma.SigmaTestBase;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * FormulaPreprocessor tests not focused on findExplicitTypes( ), but requiring that the KBs be loaded.
 */
public class FormulaPreprocessorIntegrationTest extends SigmaTestBase {

    /**
     * NOTE: If this test fails, you need to load Mid-level-ontology.kif. One way to do this would be to edit
     * your config.xml file by putting this line under "<kb name="SUMO" >":
     *    <constituent filename="/Users/geraldkurlandski/Documents/workspace_Sigma/run/KBs/Mid-level-ontology.kif" />
     */
    @Test
    public void testComputeVariableTypesTypicalPart()     {
        String stmt =   "(=> " +
                            "(typicalPart ?X ?Y) " +
                            "(subclass ?Y Object))";

        Formula f = new Formula();
        f.read(stmt);

        FormulaPreprocessor formulaPre = new FormulaPreprocessor();
        HashMap<String, HashSet<String>> actual = formulaPre.computeVariableTypes(f, kb);

        Map<String, HashSet<String>> expected = Maps.newHashMap();
        HashSet<String> set1 = Sets.newHashSet("SetOrClass", "Object+");
        expected.put("?Y", set1);
        expected.put("?X", Sets.newHashSet("Object+"));

        assertEquals(expected, actual);
    }

}