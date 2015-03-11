package com.articulate.sigma.test;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.net.URL;
import java.util.Collection;
import java.util.ListIterator;
import java.util.function.Function;

/**
 * Created by aholub on 3/9/15.
 */
public class JsonReader {

    public static <T> Collection<T> transform(String resourcePath, Function<JSONObject, T> transformer) {
        Collection<T> result = Lists.newArrayList();

        URL jsonTests = Resources.getResource(resourcePath);
        System.out.println("Reading JSON file: " + jsonTests.getPath());
        JSONParser parser = new JSONParser();
        try {
            Object obj = parser.parse(Resources.toString(jsonTests, Charsets.UTF_8));
            JSONArray jsonObject = (JSONArray) obj;
            ListIterator<JSONObject> li = jsonObject.listIterator();
            while (li.hasNext()) {
                JSONObject jo = li.next();
                result.add(transformer.apply(jo));
            }
        } catch (Exception e) {
            System.out.println("Parse exception reading: " + resourcePath);
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        return result;
    }
}
