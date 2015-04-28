package com.articulate.sigma.inference;

import com.articulate.sigma.KBmanager;
import com.articulate.sigma.nlp.TextFileUtil;
import com.articulate.sigma.semRewrite.Interpreter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * Created by sserban on 3/25/15.
 */
@RunWith(Parameterized.class)
public class InferenceWikiTest {

    private static Interpreter interpreter;
    public static final Set<String> files = new HashSet();


    @Parameterized.Parameter(value= 0)
    public String filename;
    @Parameterized.Parameter(value= 1)
    public String query;
    @Parameterized.Parameter(value= 2)
    public String answer;

    /**************************************************************************************************
     *
     */
    @BeforeClass
    public static void initialize() throws IOException {

        KBmanager.getMgr().initializeOnce();
        interpreter = new Interpreter();
        interpreter.inference = true;
        interpreter.autoir = false;
        interpreter.initialize();
    }

    /***********************************************************************************************
     *
     */
    @Test
    public void test() {

        //cache a knowledge base for each file if possible, a la TFIDFWikiTest
        if(!files.contains(filename)) {
            //reading the lines
            List<String> lines = null;
            try {
                lines = TextFileUtil.readLines("miscellaneous/" + filename, true);
                files.add(filename);
            } catch (IOException e) {
                System.out.println("Couldn't read document: " + filename + ". Exiting");
                return;
            }

            //loading the assertions
            interpreter.question = false;
            for (String line:lines) {
                System.out.println("\nAsserting: " + line);
                String response = interpreter.interpretSingle(line);
                System.out.println("Response: " + response);
            }

        }

        //asking the questions
        interpreter.question = true;
        System.out.println("\nQuestion: " + query);
        String actual = interpreter.interpretSingle(query);
        System.out.println("Answer: " + answer);
        assertEquals(answer.replaceAll("\\W$"," ").trim(), actual.replaceAll("\\W$"," ").trim());

    }

    /******************************************************************************************************
     *
     * @return
     */
    @Parameterized.Parameters(name="{0}:{1}")
    public static Collection<Object[]> prepare(){

        ArrayList<Object[]> result = new ArrayList<Object[]>();
        File jsonTestFile = new File("test/corpus/java/resources/miscellaneous/inferenceWiki.json");
        String filename = jsonTestFile.getAbsolutePath();
        JSONParser parser = new JSONParser();
        try {
            Object obj = parser.parse(new FileReader(filename));
            JSONArray jsonObject = (JSONArray) obj;
            ListIterator<JSONObject> li = jsonObject.listIterator();
            while (li.hasNext()) {
                JSONObject jo = li.next();
                String fname = (String) jo.get("file");
                String query = (String) jo.get("query");
                String answer = (String) jo.get("answer");
                result.add(new Object[]{fname,query,answer});
            }
        }
        catch (FileNotFoundException e) {
            System.out.println("Error in InferenceWikiTest.prepare(): File not found: " + filename);
            System.out.println(e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("FileNotFoundException");
        }
        catch (IOException e) {
            System.out.println("Error in InferenceWikiTest.prepare(): IO exception reading: " + filename);
            System.out.println(e.getMessage());
            e.printStackTrace();
            e.printStackTrace();
            throw new RuntimeException("IOException");
        }
        catch (ParseException e) {
            System.out.println("Error in InferenceWikiTest.prepare(): Parse exception reading: " + filename);
            System.out.println(e.getMessage());
            throw new RuntimeException("ParseException");
        }
        catch (Exception e) {
            System.out.println("Error in InferenceWikiTest.prepare(): Parse exception reading: " + filename);
            System.out.println(e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Exception");
        }
        return result;
    }


}