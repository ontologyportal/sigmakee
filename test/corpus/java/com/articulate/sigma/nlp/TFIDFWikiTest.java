package com.articulate.sigma.nlp;

import com.google.common.collect.Maps;
import edu.stanford.nlp.util.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.*;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class TFIDFWikiTest {

    //public static BufferedWriter bufferedWriter = null;

    public static final Map<String, TFIDF> files = Maps.newHashMap();

    private String filename;
    private String query;
    private String answer;

    public TFIDFWikiTest(String filename, String query, String answer) {
        this.filename = filename;
        this.query = query;
        this.answer = answer;
    }

    @Test
    public void test() throws IOException {
        TFIDF cb;
        if (files.containsKey(filename))
            cb = files.get(filename);
        else {
            List<String> documents = null;
            try {
                documents = TextFileUtil.readLines("cmuWiki/" + filename, true);
            } catch (IOException e) {
                System.out.println("Couldn't read document: " + filename + ". Exiting");
                return;
            }
            cb = new TFIDF(documents, "resources/stopwords.txt", true);
            files.put(filename, cb);
        }
        String actual = cb.matchInput(query);
        String[] splitted = actual.split(":");
        //actual = StringUtils.join(Arrays.copyOfRange(splitted, 1, splitted.length), ":");

        assertMatchesAny(answer, actual);
    }

    private void assertMatchesAny(String answer, String actual) {
        String[] possibleAnswsers = answer.split("\\|");
        boolean foundMatch = false;
        for (String a : possibleAnswsers) {
            if (actual.equals(a)) {
                foundMatch = true;
                break;
            }
        }
        if (!foundMatch) {
            fail("\"" + answer + "\" not found in actual: \n\t" + actual);
        }
    }

    @Parameterized.Parameters(name="{0}:{1}")
    public static Collection<Object[]> prepare(){

        ArrayList<Object[]> result = new ArrayList<Object[]>();
        File jsonTestFile = new File("test/corpus/java/resources/cmuWiki/cmuWiki.json");
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
        return result;
    }


}