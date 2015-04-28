package com.articulate.sigma.semRewrite;

import com.articulate.sigma.Formula;
import com.articulate.sigma.IntegrationTestBase;
import com.articulate.sigma.KBmanager;
import com.articulate.sigma.test.JsonReader;
import org.json.simple.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.io.IOException;
import java.util.Collection;
import java.util.Objects;

import static org.junit.Assert.assertTrue;

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
    public static void initInterpreter() throws IOException {
        
        interp = new Interpreter();
        KBmanager.getMgr().initializeOnce();
        interp.initialize();
    }
    
    /** ***************************************************************
     */
    @Parameters(name="{0}")
    public static Collection<Object[]> prepare() {
        
        //return JsonReader.transform("resources/translation_tiny.json", (JSONObject jo) -> {
        return JsonReader.transform("QA/json/translation_tests.json", (JSONObject jo) -> {
            String text = (String) jo.get("text");
            //String tokens = (String) jo.get("tokens");
            //String type = (String) jo.get("type");
            String kif = (String) jo.get("kif");
            return new Object[]{text,kif};
        });
    }

    /** ***************************************************************
     */
    private String unify(String data) {
        
        return data.replaceAll("\\n", "").replaceAll("\\s*\\)", ")").replaceAll("\\s*\\(", "(").trim();
    }
    
    /** ***************************************************************
     */
    @Test
    public void test() {
        
        String actual = interp.interpretSingle(fInput);
        // Just to have beautiful output
        if (!Objects.equals(unify(fExpected), unify(actual))) {
            boolean passed = (new Formula(fExpected)).logicallyEquals(new Formula(actual));
            System.out.println("Input: " + fInput);
            System.out.println("Expected: " + fExpected);
            System.out.println("Actual: " + actual);
            if (!passed)
                System.out.println("****** FAIL ******");
            else
                System.out.println("pass");
            assertTrue("The following should be equal: \n" + fExpected + "\n and \n" + actual, passed);
        }
    }
}

