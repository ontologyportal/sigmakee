import com.articulate.sigma.*;
import com.articulate.sigma.trans.*;
import com.articulate.sigma.parsing.*;
import java.util.*;

public class VerifyCompOp {
    public static void main(String[] args) {
        KBmanager.getMgr().initializeOnce();
        KB kb = KBmanager.getMgr().getKB("SUMO");
        SUMOtoTFAform.kb = kb;
        SUMOtoTFAform.initOnce();
        SUMOtoTFAform.targetLeo = true;
        SUMOtoTFAform.debug = true;
        
        // V__ITEMFROM2 is inferred as Integer, AdditionFn returns RealNumber
        Formula f = new Formula("(equal ?ITEMFROM2 (AdditionFn ?ITEMFROM1 ?PRIORFROM2))");

        Map<String, Set<String>> varmap = new HashMap<>();        varmap.put("?ITEMFROM2", new HashSet<>(Arrays.asList("Integer")));
        varmap.put("?ITEMFROM1", new HashSet<>(Arrays.asList("Integer")));
        varmap.put("?PRIORFROM2", new HashSet<>(Arrays.asList("Integer")));
        SUMOtoTFAform.setVarmap(varmap);
        
        String res = SUMOtoTFAform.processRecurse(f, "Entity");
        System.out.println("Result: " + res);
        
        if (res.contains("$to_real(V__ITEMFROM2)")) {
            System.out.println("SUCCESS: ITEMFROM2 promoted to $to_real");
        } else {
            System.out.println("FAILURE: ITEMFROM2 not promoted");
        }
    }
}
