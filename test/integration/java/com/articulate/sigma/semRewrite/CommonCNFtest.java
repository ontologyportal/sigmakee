package com.articulate.sigma.semRewrite;/*
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

import com.articulate.sigma.IntegrationTestBase;
import com.articulate.sigma.SigmaTestBase;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class CommonCNFtest extends IntegrationTestBase {

    @Test
    public void testCase1() {

        Map<Integer, String> map = new HashMap<Integer, String>();
        map.put(0, "Mary drives the car to the store.");
        map.put(1, "John flies a plane to the airport.");
        Map<Integer, CNF> res = CommonCNFUtil.generateCNFForStringSet(map);
        CNF cnf = CommonCNFUtil.findOneCommonCNF(res.values());
        Assert.assertTrue(cnf.clauses.size() > 6);
    }
}
