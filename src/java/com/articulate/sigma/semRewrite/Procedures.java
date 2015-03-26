package com.articulate.sigma.semRewrite;

/*
Copyright 2014-2015 IPsoft

Author: Adam Pease adam.pease@ipsoft.com
Author: Sofia Athenikos sofia.athenikos@ipsoft.com

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

import com.articulate.sigma.*;

public class Procedures {
    
    /** ***************************************************************
     * CELT classes which are not SUMO classes, like "person"
     */
    public static String isCELTclass(Clause c) {
    
        KB kb = KBmanager.getMgr().getKB("SUMO");  
        //System.out.println("INFO in Procedures.isCELTclass(): " + c);
        if (kb == null) {
            //if (c.arg1.equals("River") && c.arg2.equals("Object"))
            //    return "true";
            return "false";
        }
        //System.out.println("INFO in Procedures.isCELTclass(): " + kb.isSubclass(c.arg1, c.arg2));

        if (c.arg2.equals("Person"))
            if (kb.isSubclass(c.arg1, "Human") || kb.isSubclass(c.arg1, "SocialRole"))
                return "true";
            else
                return "false";
        else if (c.arg2.equals("Time"))
            if (kb.isSubclass(c.arg1, "TimeMeasure") || kb.isSubclass(c.arg1, "Process"))
                return "true";
            else
                return "false";
        else if (kb.isSubclass(c.arg1, c.arg2))
            return "true";
        else
            return "false";
    }
   
    /** ***************************************************************
     */
    public static String isSubclass(Clause c) {
        
        KB kb = KBmanager.getMgr().getKB("SUMO");  
        //System.out.println("INFO in Procedures.isSubclass(): " + c);
        //System.out.println("INFO in Procedures.isSubclass(): " + kb.isSubclass(c.arg1, c.arg2));
        if (kb.isSubclass(c.arg1, c.arg2))
            return "true";
        else
            return "false";
    }
    
    /** ***************************************************************
     */
    public static String isInstanceOf(Clause c) {
        
        KB kb = KBmanager.getMgr().getKB("SUMO");
        //System.out.println("INFO in Procedures.isInstanceOf(): " + c);
        //System.out.println("INFO in Procedures.isInstanceOf(): " + kb.isInstanceOf(c.arg1, c.arg2));
        if (kb.isInstanceOf(c.arg1, c.arg2))
            return "true";
        else
            return "false";
    }

    /** ***************************************************************
     */
    public static String isSubAttribute(Clause c) {

        KB kb = KBmanager.getMgr().getKB("SUMO");
        if (kb.isSubAttribute(c.arg1, c.arg2)) {
            return "true";
        } else {
            return "false";
        }
    }

}
