package com.articulate.sigma.semRewrite;

import com.articulate.sigma.*;

public class Procedures {

    public static String execProc(Clause c) {
        
        if (c.pred.equals("isCELTclass")) {
            return isCELTclass(c);         
        }
        return "";
    }
    
    public static String isCELTclass(Clause c) {
    
        KB kb = KBmanager.getMgr().getKB("SUMO");  
        if (kb.isSubclass(c.arg1, c.arg2))
            return "true";
        else
            return "false";
    }
}
