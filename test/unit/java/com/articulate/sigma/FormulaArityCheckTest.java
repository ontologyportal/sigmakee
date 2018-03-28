package com.articulate.sigma;
/*
Copyright 2014-2015 IPsoft

Author: Peigen You Peigen.You@ipsoft.com

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

import junit.framework.AssertionFailedError;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class FormulaArityCheckTest extends UnitTestBase {

    @Test
    public void testArityCheck1() {

        String input = "(=>\n" +
                "   (and\n" +
                "      (domainSubclass ?REL ?NUMBER ?CLASS1)\n" +
                "      (domainSubclass ?REL ?NUMBER ?CLASS2))\n" +
                "   (or\n" +
                "      (subclass ?CLASS1 ?CLASS2)\n" +
                "      (subclass ?CLASS2 ?CLASS1)))";
        Formula f = new Formula();
        f.read(input);
        String output = PredVarInst.hasCorrectArity(f, kb);
        assertNull(output);
    }

    @Test
    public void testArityCheck2() throws AssertionFailedError {

        String input = "(=>\n" +
                "   (and\n" +
                "      (subrelation ?REL1 ?REL2 Car)\n" +
                "      (domainSubclass ?REL2 ?NUMBER ?CLASS1))\n" +
                "   (domainSubclass ?REL1 ?NUMBER ?CLASS1))";
        Formula f = new Formula();
        f.read(input);
        String output = PredVarInst.hasCorrectArity(f, kb);
        assertNotNull(output);
    }

}
