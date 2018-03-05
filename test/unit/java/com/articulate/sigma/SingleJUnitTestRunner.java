package com.articulate.sigma;

/**
 * Created by apease on 2/16/18 from https://stackoverflow.com/questions/9288107/run-single-test-from-a-junit-class-using-command-line
 *
 *  Usage:
 * java -cp path/to/testclasses:path/to/junit-4.8.2.jar SingleJUnitTestRunner
 com.mycompany.product.MyTest#testB

 */

import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;

public class SingleJUnitTestRunner {

    /** *************************************************************
     */
    public static void main(String[] args) {

        try {
            String[] classAndMethod = args[0].split("#");
            System.out.println("running test: " + classAndMethod[0] + " : " + classAndMethod[1]);
            Request request = Request.method(Class.forName(classAndMethod[0]),
                    classAndMethod[1]);

            Result result = new JUnitCore().run(request);
            System.exit(result.wasSuccessful() ? 0 : 1);
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}