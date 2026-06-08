package com.articulate.sigma;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.After;
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

/***************************************************************
 * Created by sserban on 3/1/15.
 */
@RunWith(Parameterized.class)
public class FormulaLogicalEqualityTest extends UnitTestBase {

    private static final String TEST_FILE_NAME = "formula_logical_equality_tests.json";

    private static long totalExecutionTime;
    private static int testCount;

    @Parameterized.Parameter(value= 0)
    public String f1Text;
    @Parameterized.Parameter(value= 1)
    public String f2Text;
    @Parameterized.Parameter(value= 2)
    public boolean areEqual;

    /***************************************************************
     */
    @Parameterized.Parameters(name = "{0}")
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
                boolean equal = (boolean) jo.get("equal");
                result.add(new Object[]{f1, f2, equal});
            }

            testCount = 0;
            totalExecutionTime = 0;
            System.out.println("Loaded " + jsonObject.size() + " tests.");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /***************************************************************
     */
    @After
    public void performanceReport() {

       System.out.println("\nFormulaLogicalEqualityTest: \nA total of " +
               testCount + " tests ran with an average of " +
               ((totalExecutionTime/testCount) / 1000000) +
               " milisecond execution time per test.\n");
    }

    /***************************************************************
     */
    @Test
    public void test() {

        //Formula.debug = true;
        Formula f1 = new Formula();
        f1.read(f1Text);
        Formula f2 = new Formula();
        f2.read(f2Text);

        long start = System.nanoTime();
        boolean comparisonResult = f1.logicallyEquals(f2);
        //boolean comparisonResult = f1.unifyWith(f2);
        long stop = System.nanoTime();
        totalExecutionTime += (stop - start);
        testCount++;
        System.out.println("FormulaLogicalEqualityTest.test() f1: " + f1);
        System.out.println("FormulaLogicalEqualityTest.test() f2: " + f2);

        if (areEqual) {
            if (comparisonResult)
                System.out.println("FormulaLogicalEqualityTest.test() Equal: success");
            else
                System.out.println("FormulaLogicalEqualityTest.test() Not equal: fail!");
            assertTrue("The following should be equal: \n" +
                    f1.getFormula() + "\n and \n" + f2.getFormula(), comparisonResult);
        }
        else {
            if (comparisonResult)
                System.out.println("FormulaLogicalEqualityTest.test() equal: fail!");
            else
                System.out.println("FormulaLogicalEqualityTest.test() not equal: success");
            assertFalse("The following should not be equal: \n" +
                    f1.getFormula() + "\n and \n" + f2.getFormula(), comparisonResult);
        }
    }
}
