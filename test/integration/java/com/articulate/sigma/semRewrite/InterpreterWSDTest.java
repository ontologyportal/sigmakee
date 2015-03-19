package com.articulate.sigma.semRewrite;/*
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

import com.articulate.sigma.KBmanager;
import com.google.common.collect.Lists;
import junit.framework.TestCase;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class InterpreterWSDTest extends TestCase {

    @Test
    public void testFindWSD() {
        KBmanager.getMgr().initializeOnce();

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

        List<String> wsds = Interpreter.findWSD(clauses);

        /* Expected:
           attribute(AmeliaMaryEarhart-1,Female)
           sumo(Woman,AmeliaMaryEarhart-1)
           sumo(Human,AmeliaMaryEarhart-1)
           sumo(UnitedStates,American-17)
           sumo(Pilot,aviator-18)
         */
        assertEquals(5, wsds.size());
        assertTrue(wsds.toString(), wsds.contains("sumo(Human,AmeliaMaryEarhart-1)"));
        assertTrue(wsds.toString(), wsds.contains("attribute(AmeliaMaryEarhart-1,Female)"));
    }
}
