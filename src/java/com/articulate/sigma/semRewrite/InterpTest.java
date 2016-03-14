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
import java.util.ArrayList;
import java.util.Collection;
import java.util.ListIterator;
import java.util.Objects;
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
    public static Collection<Object[]> prepare() {

        //return JsonReader.transform("resources/translation_tiny.json", (JSONObject jo) -> {
        //return JsonReader.transform("miscellaneous/translation_tests.json", (JSONObject jo) -> {
        return transform("/home/apease/Relcy/ImperativeTests.json", (JSONObject jo) -> {
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

    /***************************************************************
     */
    public static void test(String fInput, String fExpected) {

        String actual = interp.interpret(fInput).get(0);
        //String actual = interp.interpretSingle(fInput);
        // Just to have beautiful output
        if (!Objects.equals(unify(fExpected), unify(actual))) {
            boolean passed = (new Formula(fExpected)).logicallyEquals(new Formula(actual));
            ArrayList<String> oneResult = new ArrayList<>();
            System.out.println("Input: " + fInput);
            oneResult.add(fInput);
            System.out.println("Expected: " + fExpected);
            oneResult.add(fExpected);
            System.out.println("Actual: " + actual);
            oneResult.add(actual);
            if (!passed) {
                System.out.println("****** FAIL ******");
                fail++;
            }
            else {
                System.out.println("pass");
                pass++;
            }
            System.out.println("The following should be equal: \n" + fExpected + "\n and \n" +
                    actual + "\n\n" + passed);
            results.add(oneResult);
        }
    }

    /****************************************************************
     */
    public static void main(String[] args) throws IOException {

        initInterpreter();
        Collection<Object[]> tests = prepare();
        for (Object[] o : tests) {
            test((String) o[0], (String) o[1]);
        }
        for (ArrayList<String> oneResult : results) {
            System.out.println("************");
            System.out.println("Input: " + oneResult.get(0));
            System.out.println("Expected: " + oneResult.get(1));
            System.out.println("Actual: " + oneResult.get(2));
        }
        System.out.println("Passed: " + pass);
        System.out.println("Failed: " + fail);
    }
}