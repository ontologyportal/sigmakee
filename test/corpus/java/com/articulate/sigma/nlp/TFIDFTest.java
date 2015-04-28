package com.articulate.sigma.nlp;

/*
Copyright 2014-2015 IPsoft

Author: Andrei Holub andrei.holub@ipsoft.com

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

import com.articulate.sigma.test.JsonReader;
import com.google.common.collect.Maps;
import junit.framework.TestCase;
import org.json.simple.JSONObject;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    @Parameterized.Parameters(name="<{0}> {1}")
    public static Collection<Object[]> prepare() {
        return JsonReader.transform("QA/json/IRtests.json", (JSONObject jo) -> {
            String fname = (String) jo.get("file");
            String query = (String) jo.get("query");
            String answer = (String) jo.get("answer");
            return new Object[]{fname, query, answer};
        });
    }

    private TFIDF getCachedTFIDF(String fileName) {
        TFIDF cb = parsedFiles.get(fileName);
        if(cb == null) {
            cb = new TFIDF("QA/textfiles" + File.separator + fileName, "resources/stopwords.txt", true);
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