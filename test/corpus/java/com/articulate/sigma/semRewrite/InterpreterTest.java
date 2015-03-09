package com.articulate.sigma.semRewrite;

import static org.junit.Assert.assertEquals;

import com.articulate.sigma.IntegrationTestBase;
import com.articulate.sigma.test.JsonReader;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.json.simple.*;

import com.articulate.sigma.KBmanager;

import java.util.*;
import java.util.function.Function;

@RunWith(Parameterized.class)
public class InterpreterTest extends IntegrationTestBase {

    public static Interpreter interp;
 
    @Parameterized.Parameter(value= 0)
    public String fInput;
    @Parameterized.Parameter(value= 1)
    public String fExpected;   
   
    /** ***************************************************************
     */
    @BeforeClass
    public static void initInterpreter() {
        interp = new Interpreter();
        KBmanager.getMgr().initializeOnce();
        interp.loadRules();
    }
    
    /** ***************************************************************
     */
    @Parameters(name="{0}")
    public static Collection<Object[]> prepare() {
        return JsonReader.transform("resources/translation_tests.json", new Function<JSONObject, Object[]>() {
            @Override
            public Object[] apply(JSONObject jo) {
                String text = (String) jo.get("text");
                //String tokens = (String) jo.get("tokens");
                //String type = (String) jo.get("type");
                String kif = (String) jo.get("kif");
                return new Object[]{text,kif};
            }
        });
    }

    private String unify(String data) {
        return data.replaceAll("\\n", "").replaceAll("\\s*\\)", ")").replaceAll("\\s*\\(", "(").trim();
    }
    
    /** ***************************************************************
     */
    @Test
    public void test() {
        String actual = interp.interpretSingle(fInput);
        // Just to have beautiful output
        if(!Objects.equals(unify(fExpected), unify(actual))) {
            assertEquals(fExpected, actual);
        }
    }
}

