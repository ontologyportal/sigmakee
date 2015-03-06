package com.articulate.sigma.nlp;

import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import junit.framework.TestCase;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static junit.framework.TestCase.assertEquals;

/**
 * Created by aholub on 3/6/15.
 */
@RunWith(Parameterized.class)
public class TFIDFTest extends TestCase {

    private static Map<String, TFIDF> parsedFiles = Maps.newHashMap();

    @Parameterized.Parameter(value= 0)
    public String fileName;
    @Parameterized.Parameter(value= 1)
    public String query;
    @Parameterized.Parameter(value= 2)
    public String answer;

    @AfterClass
    public static void cleanUp() {
        parsedFiles.clear();
    }

    @Parameterized.Parameters(name="{0}: {1}")
    public static Collection<Object[]> prepare() {
        ArrayList<Object[]> result = new ArrayList<>();

        URL irTests = Resources.getResource("resources/IRtests.json");
        String filename = irTests.getPath();

        //System.out.println("INFO in TFIDF.prepare(): reading: " + filename);
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
                //System.out.println("INFO in TFIDF.prepare(): " + fname + " " + query + " " + answer);
                result.add(new Object[]{fname,query,answer});
            }
        }
        catch (FileNotFoundException e) {
            System.out.println("Error in TFIDF.prepare(): File not found: " + filename);
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        catch (IOException e) {
            System.out.println("Error in TFIDF.prepare(): IO exception reading: " + filename);
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        catch (ParseException e) {
            System.out.println("Error in TFIDF.prepare(): Parse exception reading: " + filename);
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        catch (Exception e) {
            System.out.println("Error in TFIDF.prepare(): Parse exception reading: " + filename);
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        //System.out.println(result);
        return result;
    }


    @Test
    public void test() {
        TFIDF cb = parsedFiles.get(fileName);
        if(cb == null) {
            cb = new TFIDF(fileName);
            parsedFiles.put(fileName, cb);
        }
        String actual = cb.matchInput(query);
        Pattern p = Pattern.compile(answer.toLowerCase());
        Matcher m = p.matcher(actual.toLowerCase());
        if(!m.find()) {
            assertEquals(answer, actual);
        }
    }
}