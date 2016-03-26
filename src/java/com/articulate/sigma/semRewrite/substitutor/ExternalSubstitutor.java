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
package com.articulate.sigma.semRewrite.substitutor;

import com.articulate.sigma.KBmanager;
import com.articulate.sigma.semRewrite.InterpTest;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.Label;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

public class ExternalSubstitutor extends SimpleSubstitutorStorage {

    public class Segmentation {
        public ArrayList<String> segments = new ArrayList<>();
        public ArrayList<String> types = new ArrayList<>();

        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("segments: " + segments.toString());
            sb.append("types: " + types.toString());
            return sb.toString();
        }
    }

    // cache of remote system responses
    public static Map<String, Segmentation> cache = new HashMap<>();

    /*************************************************************
     */
    public ExternalSubstitutor(List<CoreLabel> labels) {

        initialize(labels);
    }

    /****************************************************************
     * {
     * "query": "blah blah blah",
     *      "annotatations": [
     *          {
     *              "types": [
     *                  "...",
     *                  ...
     *              ],
     *              "segmentation": [
     *                  "...",
     *                  ...
     *              ],
     *          }
     *      ],
     * }
     */
    private Segmentation parseNERJson(String json) {

        try {
            Segmentation result = new Segmentation();
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(json);
            JSONObject jsonObject = (JSONObject) obj;
            System.out.println("ExternaSubstitutor.parseNERJson(): Reading JSON file: " + jsonObject);

            JSONArray anno = (JSONArray) jsonObject.get("annotatations");
            System.out.println("ExternaSubstitutor.parseNERJson(): annotations: " + anno);
            JSONObject msg = (JSONObject) anno.get(0);
            JSONArray typeObj = (JSONArray) msg.get("types");
            Iterator<String> iterator = typeObj.iterator();
            ArrayList<String> types = new ArrayList<String>();
            while (iterator.hasNext()) {
                result.types.add(iterator.next());
            }

            JSONArray msg2 = (JSONArray) msg.get("segmentation");
            Iterator<String> iterator2 = msg2.iterator();
            ArrayList<String> segments = new ArrayList<String>();
            while (iterator2.hasNext()) {
                result.segments.add(iterator2.next());
            }
            return result;
        }
        catch (ParseException pe) {
            System.out.println("Error in ExternaSubstitutor.prepare(): parseException: " + json);
            pe.printStackTrace();
            return null;
        }
    }

    /* *************************************************************
     * Load a file consisting of previous responses from the remote NER
     * server.
     */
    private void loadCache() {

        if (cache.keySet().size() > 0)
            return;  // don't load the cache multiple times
        String resourcePath = System.getenv("SIGMA_HOME") + File.separator + "KBs" +
                File.separator + "nerCache.json";
        try {
            File f = new File(resourcePath);
            if (!f.exists())
                return;
            FileReader fr = new FileReader(f);
            System.out.println("ExternaSubstitutor.loadCache(): Reading JSON file: " + resourcePath);
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(fr);
            JSONObject jsonObject = (JSONObject) obj;
            JSONArray msg = (JSONArray) jsonObject.get("cache");

            Iterator<JSONObject> iterator = msg.iterator();
            while (iterator.hasNext()) {
                JSONObject oneQuery = iterator.next();
                String query = (String) oneQuery.get("query");
                JSONArray types = (JSONArray) oneQuery.get("types");
                JSONArray anno = (JSONArray) oneQuery.get("segmentation");
                Segmentation seg = new Segmentation();
                seg.segments.addAll(anno);
                seg.types.addAll(types);
                cache.put(query, seg);
            }
        }
        catch (Exception e) {
            System.out.println("Parse exception reading: " + resourcePath);
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    /***************************************************************
     */
    private void saveCache() {

        String resourcePath = System.getenv("SIGMA_HOME") + File.separator + "KBs" +
                File.separator + "nerCache.json";
        try {
            FileWriter fw = new FileWriter(new File(resourcePath));
            System.out.println("Writing JSON file: " + resourcePath);
            JSONObject jsonObject = new JSONObject();
            JSONArray cacheJson = new JSONArray();

            for (String s : cache.keySet()) {
                JSONObject queryAndNER = new JSONObject();
                queryAndNER.put("query", s);

                Segmentation seg = cache.get(s);
                JSONArray seglist = new JSONArray();
                JSONArray typelist = new JSONArray();
                for (String segment : seg.segments) {
                    seglist.add(segment);
                }
                for (String segment : seg.types) {
                    typelist.add(segment);
                }
                queryAndNER.put("segmentation", seglist);
                queryAndNER.put("types", typelist);
                cacheJson.add(queryAndNER);
            }
            jsonObject.put("cache", cacheJson);
            fw.write(jsonObject.toJSONString());
            fw.flush();
            fw.close();
        }
        catch (Exception e) {
            System.out.println("Exception writing: " + resourcePath);
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    /* *************************************************************
     * Collects information froma remote service about continuous noun
     * sequences like "Garry Bloom", "Tim Buk Tu"
     */
    private static String toURLSentence(List<CoreLabel> labels) {

        StringBuffer sb = new StringBuffer();
        for (CoreLabel cl : labels) {
            System.out.println("Info in ExternalSubstitutor.toSentence(): label: " + cl);
            if (!sb.toString().equals(""))
                sb.append("+");
            sb.append(cl.toString());
        }
        return sb.toString();
    }

    /* *************************************************************
     * convert a string consisting of space-delimited words into a
     * CoreLabelSequence
     */
    private static List<CoreLabel> stringToCoreLabelList(String s) {

        ArrayList<CoreLabel> al = new ArrayList<>();
        String[] splits = s.split(" ");
        for (String word : splits) {
            CoreLabel cl = new CoreLabel();
            cl.setValue(word);
            al.add(cl);
        }
        return al;
    }

    /* *************************************************************
     * convert a string consisting of space-delimited words into a
     * CoreLabelSequence
     */
    private static CoreLabelSequence stringToCoreLabelSeq(String s) {

        List<CoreLabel> al = stringToCoreLabelList(s);
        CoreLabelSequence result = new CoreLabelSequence(al);
        return result;
    }

    /* *************************************************************
     */
    private Map<CoreLabelSequence, CoreLabelSequence> segToCoreLabel(Segmentation seg) {

        Map<CoreLabelSequence, CoreLabelSequence> result = new HashMap<>();
        for (String s : seg.segments) {
            if (s.indexOf(' ') > -1) {
                CoreLabelSequence cls = stringToCoreLabelSeq(s);
                result.put(cls,cls);
            }
        }
        return result;
    }

    /* *************************************************************
     * Given a list of tokens from a single sentence, collects information from a remote
     * service about continuous noun sequences like "Garry Bloom",
     * "Tim Buk Tu"
     */
    private void initialize(List<CoreLabel> labels) {

        System.out.println("Info in ExternalSubstitutor.initialize(): labels: " + labels);
        Segmentation seg = null;
        loadCache();
        String nerurl = "";
        try {
            String sentence = toURLSentence(labels);
            if (cache.containsKey(sentence)) {
                seg = cache.get(sentence);
                System.out.println("Info in ExternaSubstitutor.initialize(): " + seg);
            }
            else {
                System.out.println("Info in ExternalSubstitutor.initialize(): sentence: " + sentence);
                nerurl = System.getenv("NERURL") + "query=" + sentence + "&limit=1";
                System.out.println("Info in ExternalSubstitutor.initialize(): URL: " + nerurl);
                URL myURL = new URL(nerurl);

                URLConnection yc = myURL.openConnection();
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        yc.getInputStream()));
                String JSONString = "";
                String inputLine;
                while ((inputLine = in.readLine()) != null)
                    JSONString += inputLine;
                System.out.println("Info in ExternalSubstitutor.initialize(): JSON: " + JSONString);
                in.close();
                seg = parseNERJson(JSONString);
                System.out.println("Info in ExternaSubstitutor.initialize(): " + seg);
                cache.put(sentence,seg);
                saveCache();
            }
        }
        catch (MalformedURLException e) {
            System.out.println("Error in ExternaSubstitutor.initialize(): new URL() failed: " + nerurl);
            e.printStackTrace();
        }
        catch (IOException e) {
            System.out.println("Error in ExternaSubstitutor.initialize(): IOException: " + nerurl);
            e.printStackTrace();
        }
        Map<CoreLabelSequence, CoreLabelSequence> groupsFull = segToCoreLabel(seg);
        // create structures like {[Uptown-5, Funk-6]=[Uptown-5, Funk-6], [Apple-8, Music-9]=[Apple-8, Music-9]}
        addGroups(groupsFull);
    }

    /****************************************************************
     */
    public static void main(String[] args) throws IOException {

        ExternalSubstitutor es = new ExternalSubstitutor(stringToCoreLabelList("i want to watch the game of thrones"));
    }
}