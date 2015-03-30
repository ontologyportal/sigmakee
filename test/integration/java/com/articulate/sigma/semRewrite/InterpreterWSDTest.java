/*
Copyright 2014-2015 IPsoft

Author: Andrei Holub andrei.holub@ipsoft.com

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program ; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston,
MA  02111-1307 USA 
*/
package com.articulate.sigma.semRewrite;

import com.articulate.sigma.IntegrationTestBase;
import com.articulate.sigma.KBmanager;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertThat;

public class InterpreterWSDTest extends IntegrationTestBase {

    ArrayList<String> clauses = Lists.newArrayList("root(ROOT-0, aviator-18)"
            , "nn(Earhart-3, Amelia-1)", "nn(Earhart-3, Mary-2)"
            , "nsubj(aviator-18, Earhart-3)"
            , "dep(Earhart-3, July-5)"
            , "num(July-5, 24-6)", "num(July-5, 1897-8)"
            , "dep(July-5, July-10)"
            , "num(July-10, 2-11)", "num(July-10, 1937-13)"
            , "cop(aviator-18, was-15)"
            , "det(aviator-18, an-16)"
            , "amod(aviator-18, American-17)");
    
    public static Map<Integer, String> posMap = Maps.newHashMap();

    static {
        posMap.put(1, "NNP");
        posMap.put(2, "NNP");
        posMap.put(3,"NNP");
        posMap.put(4, "-LRB-");
        posMap.put(5, "NNP");
        posMap.put(6, "CD");
        posMap.put(7, ",");
        posMap.put(8, "CD");
        posMap.put(9, ":");
        posMap.put(10, "NNP");
        posMap.put(11, "CD");
        posMap.put(12, ",");
        posMap.put(13, "CD");
        posMap.put(14, "-RBR-");
        posMap.put(15, "VBD");
        posMap.put(16, "DT");
        posMap.put(17, "JJ");
        posMap.put(18, "NN");
        posMap.put(19, ".");
    }

    @BeforeClass
    public static void initClauses() {
        KBmanager.getMgr().initializeOnce();
    }

    @Test
    public void findWSD_NoGroups() {
        List<String> wsds = Interpreter.findWSD(clauses, Maps.newHashMap(), EntityTypeParser.NULL_PARSER);
        String[] expected = {
                //"names(Amelia-1,\"Amelia\")", // missed without real EntityParser information
                //"names(Mary-2,\"Mary\")",
                "sumo(DiseaseOrSyndrome,Amelia-1)", // from WordNet: Amelia
                "sumo(Woman,Mary-2)",
                "sumo(Woman,Earhart-3)",
                "sumo(UnitedStates,American-17)",
                "sumo(Pilot,aviator-18)"
        };
        assertThat(wsds, hasItems(expected));
        assertEquals(wsds.size(), expected.length);
    }

    @Test
    public void findWSD_WithGroups() {
        Interpreter.groupClauses(clauses);
        List<String> wsds = Interpreter.findWSD(clauses, Maps.newHashMap(), EntityTypeParser.NULL_PARSER);
        String[] expected = {
                //"names(AmeliaMaryEarhart-1,\"Amelia Mary Earhart\")", // missed without real EntityParser information
                "sumo(UnitedStates,American-17)",
                "sumo(Pilot,aviator-18)"
        };
        assertThat(wsds, hasItems(expected));
        assertEquals(wsds.size(), expected.length);
    }
}