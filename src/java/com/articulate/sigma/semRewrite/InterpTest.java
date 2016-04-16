package com.articulate.sigma.semRewrite;

import com.articulate.sigma.Formula;
import com.articulate.sigma.KBmanager;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import org.json.simple.parser.JSONParser;

import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.function.Function;

/*
Copyright 2014-2015 Articulate Software

Author: Adam Pease apease@articulatesoftware.com

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
public class InterpTest {
    public static Interpreter interp;
    public static int pass = 0;
    public static int fail = 0;
    public static int subsumed = 0;
    public static ArrayList<ArrayList<String>> results =  new ArrayList<ArrayList<String>>();

    /** **************************************************************
     */
    public static <T> Collection<T> transform(String resourcePath, Function<JSONObject, T> transformer) {
        Collection<T> result = Lists.newArrayList();

        try {
            FileReader fr = new FileReader(new File(resourcePath));
            System.out.println("Reading JSON file: " + resourcePath);
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(fr);
            JSONArray jsonObject = (JSONArray) obj;
            ListIterator<JSONObject> li = jsonObject.listIterator();
            while (li.hasNext()) {
                JSONObject jo = li.next();
                result.add(transformer.apply(jo));
            }
        }
        catch (Exception e) {
            System.out.println("Parse exception reading: " + resourcePath);
            System.out.println(e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Parse exception reading: " + resourcePath);
        }
        return result;
    }

    /****************************************************************
     */
    public static void initInterpreter() throws IOException {

        interp = new Interpreter();
        KBmanager.getMgr().initializeOnce();
        interp.initialize();
    }

    /****************************************************************
     */
    public static Collection<Object[]> prepare(String inputfile) {

        //return JsonReader.transform("resources/translation_tiny.json", (JSONObject jo) -> {
        //return JsonReader.transform("miscellaneous/translation_tests.json", (JSONObject jo) -> {
        return transform(KBmanager.getMgr().getPref("kbDir") + File.separator + inputfile, (JSONObject jo) -> {
            String text = (String) jo.get("text");
            //String tokens = (String) jo.get("tokens");
            //String type = (String) jo.get("type");
            String kif = (String) jo.get("kif");
            return new Object[]{text, kif};
        });
    }

    /****************************************************************
     */
    private static String unify(String data) {

        return data.replaceAll("\\n", "").replaceAll("\\s*\\)", ")").replaceAll("\\s*\\(", "(").trim();
    }

    /****************************************************************
     * if one formula has literals that are found in every other formula
     * the first subsumes the second (a little counter-intuitive).
     */
    private static boolean subsumes(String actual, String expected) {

        Formula act = new Formula(actual);
        Formula exp = new Formula(expected);
        act = act.cddrAsFormula();
        exp = exp.cddrAsFormula();
        //System.out.println("Info in InterpTest.subsumes(): actual 1: " + act);
        //System.out.println("Info in InterpTest.subsumes(): expected 1: " + exp);
        if (act == null)
            return false;
        if (exp == null)
            return false;
        act = act.carAsFormula();
        exp = exp.carAsFormula();
        //System.out.println("Info in InterpTest.subsumes(): actual 2: " + act);
        //System.out.println("Info in InterpTest.subsumes(): expected 2: " + exp);
        if (act == null)
            return false;
        if (exp == null)
            return false;
        act = act.cdrAsFormula();
        exp = exp.cdrAsFormula();
        //System.out.println("Info in InterpTest.subsumes(): actual 3: " + act);
        //System.out.println("Info in InterpTest.subsumes(): expected 3: " + exp);
        if (act == null)
            return false;
        if (exp == null)
            return false;
        List<String> expLiterals = exp.complexArgumentsToArrayList(0);
        List<String> actLiterals = act.complexArgumentsToArrayList(0);
        for (String explit : expLiterals) {
            Formula exForm = new Formula(explit);
            if (!exForm.isSimpleClause()) {
                System.out.println("Error in InterpTest.subsumes(): non-simple literal: " + exForm);
                return false;
            }
            boolean found = false;
            for (String actlit : actLiterals) {
                Formula actForm = new Formula(actlit);
                if (actForm.equals(exForm))
                    found = true;
            }
            if (!found)
                return false;
        }
        return true;
    }

    /***************************************************************
     */
    public static void testOne(String fInput, String fExpected) {

        String actual = interp.interpret(fInput).get(0);
        boolean passed = (new Formula(fExpected)).logicallyEquals(new Formula(actual));
        ArrayList<String> oneResult = new ArrayList<>();
        System.out.println("Input: " + fInput);
        oneResult.add(fInput);
        System.out.println("Expected: " + fExpected);
        oneResult.add(fExpected);
        System.out.println("Actual: " + actual);
        oneResult.add(actual);
        if (subsumes(actual,fExpected)) {
            oneResult.add("Subsumed");
            System.out.println("Subsumed");
            subsumed++;
        }
        else if (!passed) {
            oneResult.add("FAIL");
            System.out.println("****** FAIL ******");
            fail++;
        }
        else {
            oneResult.add("PASS");
            System.out.println("pass");
            pass++;
        }
        System.out.println("The following should be equal: \n" + fExpected + "\n and \n" +
                actual + "\n\n" + passed);
        results.add(oneResult);
    }

    /***************************************************************
     */
    public static void interpOne() {

        String actual = interp.interpret("Want to listen to Uptown Funk on Apple Music").get(0);
        //String actual = interp.interpret("All day I've wanted to play Uptown Funk on Apple Music").get(0);
        System.out.println("Actual: " + actual);
    }

    /***************************************************************
     */
    public static void testAll(String fname) {

        System.out.println("");
        System.out.println("****************************");
        System.out.println("");
        Collection<Object[]> tests = prepare(fname);
        for (Object[] o : tests) {
            testOne((String) o[0], (String) o[1]);
        }
        for (ArrayList<String> oneResult : results) {
            System.out.println("************");
            System.out.println("Input: " + oneResult.get(0));
            System.out.println("Expected: " + oneResult.get(1));
            System.out.println("Actual: " + oneResult.get(2));
            System.out.println("pass/fail: " + oneResult.get(3));
        }
        System.out.println("Passed: " + pass);
        System.out.println("Failed: " + fail);
        System.out.println("Subsumed: " + subsumed);
    }

    /***************************************************************
     */
    public static void testAllCSV(String fname) {

        Collection<Object[]> tests = prepare(fname);
        for (Object[] o : tests) {
            testOne((String) o[0], (String) o[1]);
        }
        for (ArrayList<String> oneResult : results) {
            System.out.print('"' + oneResult.get(0) + "\"\t");
            System.out.print('"' + oneResult.get(1) + "\"\t");
            System.out.print('"' + oneResult.get(2) + "\"\t");
            System.out.println('"' + oneResult.get(3) + "\"");
        }
        System.out.println("Passed: " + pass);
        System.out.println("Failed: " + fail);
        System.out.println("Subsumed: " + subsumed);
    }

    /****************************************************************
     */
    public static void main(String[] args) throws IOException {

        initInterpreter();
        if (args != null && args.length > 0) {
            if (args[0].equals("-o"))
                interpOne();
            if (args[0].equals("-f") && args.length > 1)
                testAllCSV(args[1]);
            else
                testAll("draft.json");
        }
        else {
            testAll("draft.json");
        }
    }
}