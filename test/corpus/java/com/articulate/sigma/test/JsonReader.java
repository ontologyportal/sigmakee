package com.articulate.sigma.test;

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
            throw new RuntimeException("Parse exception reading: " + resourcePath);
        }
        return result;
    }
}
