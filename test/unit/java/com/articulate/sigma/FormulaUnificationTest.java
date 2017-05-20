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

import static org.junit.Assert.assertTrue;

/**
 * Created by sserban on 3/1/15.
 */
@RunWith(Parameterized.class)
public class FormulaUnificationTest extends UnitTestBase {
    private static final String TEST_FILE_NAME = "formula_unification_tests.json";

    @Parameterized.Parameter(value= 0)
    public String f1Text;
    @Parameterized.Parameter(value= 1)
    public String f2Text;


    @Parameterized.Parameters
    public static Collection<Object[]> loadParamteres() {

        File jsonTestFile = new File(UnitTestBase.CONFIG_FILE_DIR, TEST_FILE_NAME);
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
                result.add(new Object[]{f1, f2});
            }
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

        assertTrue(f1.unifyWith(f2));
    }
}
