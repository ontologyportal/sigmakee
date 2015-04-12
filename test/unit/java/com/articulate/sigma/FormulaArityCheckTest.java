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

import junit.framework.Assert;
import junit.framework.AssertionFailedError;
import org.junit.Test;

public class FormulaArityCheckTest {

    KB kb;
    public FormulaArityCheckTest(){
        KBmanager kbm=KBmanager.getMgr();
        kbm.initializeOnce();
        kb=kbm.getKB("SUMO");
    }
    @Test
    public void testArityCheck1(){
        String input="(<=>\n" +
                "  (equal (BirthsPerThousandFn ?AREA (YearFn ?YEAR)) ?REALNUMBER)\n" +
                "  (and\n" +
                "    (equal (DivisionFn (PopulationFn ?AREA) 1000) ?THOUSANDS)\n" +
                "    (equal ?BIRTHCOUNT\n" +
                "      (CardinalityFn\n" +
                "        (KappaFn ?BIRTH\n" +
                "          (and\n" +
                "            (instance ?BIRTH Birth)\n" +
                "            (experiencer ?BIRTH ?INFANT)\n" +
                "            (instance ?INFANT Human)\n" +
                "            (during (WhenFn ?BIRTH) (YearFn ?YEAR))\n" +
                "            (equal (WhereFn ?BIRTH (WhenFn ?BIRTH)) ?AREA)))))\n" +
                "    (equal (DivisionFn ?BIRTHCOUNT ?THOUSANDS) ?REALNUMBER)))";
        Formula f=new Formula();
        f.read(input);
        String output=PredVarInst.hasCorrectArity(f,kb);
        Assert.assertNull(output);
    }

    @Test
    public void testArityCheck2() throws AssertionFailedError{
        String input="(<=>\n" +
                "  (equal (BirthsPerThousandFn ?AREA (YearFn ?YEAR)) ?REALNUMBER)\n" +
                "  (and\n" +
                "    (equal (DivisionFn (PopulationFn ?AREA) 1000) ?THOUSANDS)\n" +
                "    (equal ?BIRTHCOUNT\n" +
                "      (CardinalityFn\n" +
                "        (KappaFn ?BIRTH\n" +
                "          (and\n" +
                "            (instance ?BIRTH Birth ?KKK)\n" +
                "            (experiencer ?BIRTH ?INFANT ?Y)\n" +
                "            (instance ?INFANT Human)\n" +
                "            (during (WhenFn ?BIRTH ?H) (YearFn ?YEAR))\n" +
                "            (equal (WhereFn ?BIRTH (WhenFn ?BIRTH)) ?AREA)))))\n" +
                "    (equal (DivisionFn ?BIRTHCOUNT ?THOUSANDS) ?REALNUMBER)))";
        Formula f=new Formula();
        f.read(input);
        String output=PredVarInst.hasCorrectArity(f,kb);
        Assert.assertNotNull(output);
    }

}
