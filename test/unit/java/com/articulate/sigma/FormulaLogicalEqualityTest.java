package com.articulate.sigma;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ListIterator;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by sserban on 3/1/15.
 */
@RunWith(Parameterized.class)
public class FormulaLogicalEqualityTest extends UnitTestBase {
    private static final String TEST_FILE_NAME = "formula_logical_equality_tests.json";

    @Parameterized.Parameter(value= 0)
    public String f1Text;
    @Parameterized.Parameter(value= 1)
    public String f2Text;
    @Parameterized.Parameter(value= 2)
    public boolean areEqual;


    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> loadParamteres() {

        File jsonTestFile = new File(IntegrationTestBase.RESOURCES_FILE, TEST_FILE_NAME);
        JSONParser parser = new JSONParser();
        ArrayList<Object[]> result = new ArrayList<Object[]>();

        try {
            Object obj = parser.parse(new FileReader(jsonTestFile.getAbsolutePath()));
            JSONArray jsonObject = (JSONArray) obj;
            ListIterator<JSONObject> li = jsonObject.listIterator();
            while (li.hasNext()) {
                JSONObject jo = li.next();
                String f1 = (String) jo.get("f1");
                String f2 = (String) jo.get("f2");
                boolean equal = (boolean) jo.get("equal");
                result.add(new Object[]{f1, f2, equal});
            }
            System.out.println("Loaded " + jsonObject.size() + " tests.");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    @Test
    public void test() {
        Formula f1 = new Formula();
        f1.read(f1Text);
        Formula f2 = new Formula();
        f2.read(f2Text);

        if(areEqual) {
            assertTrue("The following should be equal: \n" + f1.theFormula + "\n and \n" + f2.theFormula, f1.logicallyEquals(f2));
        } else {
            assertFalse("The following should be equal: \n" + f1.theFormula + "\n and \n" + f2.theFormula, f1.logicallyEquals(f2));
        }
    }
}
