package com.articulate.sigma.semRewrite;

import com.articulate.sigma.*;

public class Procedures {
    
    // CELT classes which are not SUMO classes like "person"
    // but not implemented yet, just using SUMO classes
    public static String isCELTclass(Clause c) {
    
        KB kb = KBmanager.getMgr().getKB("SUMO");  
        //System.out.println("INFO in Procedures.isCELTclass(): " + c);
        //System.out.println("INFO in Procedures.isCELTclass(): " + kb.isSubclass(c.arg1, c.arg2));
        if (kb.isSubclass(c.arg1, c.arg2))
            return "true";
        else
            return "false";
    }
   
    public static String isSubclass(Clause c) {
        
        KB kb = KBmanager.getMgr().getKB("SUMO");  
        //System.out.println("INFO in Procedures.isSubclass(): " + c);
        //System.out.println("INFO in Procedures.isSubclass(): " + kb.isSubclass(c.arg1, c.arg2));
        if (kb.isSubclass(c.arg1, c.arg2))
            return "true";
        else
            return "false";
    }
    
    public static String isInstanceOf(Clause c) {
        
        KB kb = KBmanager.getMgr().getKB("SUMO");
        //System.out.println("INFO in Procedures.isInstanceOf(): " + c);
        //System.out.println("INFO in Procedures.isInstanceOf(): " + kb.isInstanceOf(c.arg1, c.arg2));
        if (kb.isInstanceOf(c.arg1, c.arg2))
            return "true";
        else
            return "false";
    }
}
