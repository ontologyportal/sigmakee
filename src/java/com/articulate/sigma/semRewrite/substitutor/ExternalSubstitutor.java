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
    }

    // cache of remote system responses
    public Map<String, Segmentation> cache = new HashMap<>();

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
    public static Object[] prepare(String json) {

        try {
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(json);
            JSONObject jsonObject = (JSONObject) obj;

            JSONObject anno = (JSONObject) jsonObject.get("annotations");
            JSONArray msg = (JSONArray) anno.get("types");
            Iterator<String> iterator = msg.iterator();
            Collection<String> types = new ArrayList<String>();
            while (iterator.hasNext()) {
                types.add(iterator.next());
            }

            JSONArray msg2 = (JSONArray) anno.get("segmentation");
            Iterator<String> iterator2 = msg2.iterator();
            Collection<String> segmentation = new ArrayList<String>();
            while (iterator2.hasNext()) {
                segmentation.add(iterator2.next());
            }
            return new Object[]{types, segmentation};
        }
        catch (ParseException pe) {
            System.out.println("Error in ExternaSubstitutor.prepare(): parseException: " + json);
            pe.printStackTrace();
            return null;
        }
    }

    /* *************************************************************
     */
    private void loadCache() {

        String resourcePath = KBmanager.getMgr().getPref("kbDir") + File.separator + "nerCache.json";
        try {
            FileReader fr = new FileReader(new File(resourcePath));
            System.out.println("Reading JSON file: " + resourcePath);
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

        String resourcePath = KBmanager.getMgr().getPref("kbDir") + File.separator + "nerCache.json";
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
    private String toSentence(List<CoreLabel> labels) {

        StringBuffer sb = new StringBuffer();
        for (CoreLabel cl : labels) {
            if (!sb.toString().equals(""))
                sb.append(" ");
            sb.append(cl.originalText());
        }
        return sb.toString();
    }

    /* *************************************************************
     * Collects information froma remote service about continuous noun
     * sequences like "Garry Bloom", "Tim Buk Tu"
     */
    private void initialize(List<CoreLabel> labels) {

        loadCache();
        String nerurl = "";
        try {
            String sentence = toSentence(labels);
            if (cache.containsKey(sentence)) {

            }
            else {
                nerurl = System.getenv("NERURL" + "?query=" + sentence + "&limit=1");
                URL myURL = new URL(nerurl);

                URLConnection yc = myURL.openConnection();
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        yc.getInputStream()));
                String JSONString = "";
                String inputLine;
                while ((inputLine = in.readLine()) != null)
                    JSONString += inputLine;
                in.close();
                Object[] result = prepare(JSONString);
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
        Map<CoreLabelSequence, CoreLabelSequence> groupsFull = parseGroupsAndCollectRoots(labels);
        addGroups(groupsFull);
    }

    /* *************************************************************
     */
    private Map<CoreLabelSequence, CoreLabelSequence> parseGroupsAndCollectRoots(List<CoreLabel> labels) {

        System.out.println("Info in ExternalSubstitutor.parseGroupsAndCollectRoots(): " + labels);
        Map<CoreLabelSequence, CoreLabelSequence> sequences = Maps.newHashMap();
        CoreLabel firstLabel = null;
        List<CoreLabel> sequence = Lists.newArrayList();
        for (CoreLabel label : labels) {
            if (firstLabel != null
                    && ("NNP".equals(label.tag()) && Objects.equals(label.tag(), firstLabel.tag()))) {
                sequence.add(label);
            }
            else {
                if (sequence.size() > 1) {
                    CoreLabelSequence s = new CoreLabelSequence(sequence);
                    sequences.put(s, s);
                }
                firstLabel = label;
                sequence = Lists.newArrayList(firstLabel);
            }
        }
        if (sequence.size() > 1) {
            CoreLabelSequence s = new CoreLabelSequence(sequence);
            sequences.put(s, s);
        }

        return sequences;
    }

    /****************************************************************
     */
    public static void main(String[] args) throws IOException {

    }
}