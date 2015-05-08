package com.articulate.sigma.nlp;

import com.articulate.sigma.test.JsonReader;
import com.google.common.collect.Maps;
import junit.framework.TestCase;
import org.json.simple.JSONObject;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by areed on 3/19/15.
 */
@RunWith(Parameterized.class)
public class AmeliaTFIDFTest extends TestCase {

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

    @Parameterized.Parameters(name="<{0}> {1}")
    public static Collection<Object[]> prepare() {
        return JsonReader.transform("miscellaneous/inferenceWiki.json", (JSONObject jo) -> {
            String fname = (String) jo.get("file");
            String query = (String) jo.get("query");
            String answer = (String) jo.get("answer");
            return new Object[]{fname, query, answer};
        });
    }

    private TFIDF getCachedTFIDF(String fileName) {
        TFIDF cb = parsedFiles.get(fileName);
        if(cb == null) {
            cb = new TFIDF("miscellaneous" + File.separator + fileName, "testfiles/stopwords.txt", true);
            parsedFiles.put(fileName, cb);
        }
        return cb;
    }

    @Test
    public void test() {
        TFIDF cb = getCachedTFIDF(fileName);
        String actual = cb.matchInput(query);
        Pattern p = Pattern.compile(answer.toLowerCase());
        Matcher m = p.matcher(actual.toLowerCase());
        if(!m.find()) {
            assertEquals(answer, actual);
        }
    }
}