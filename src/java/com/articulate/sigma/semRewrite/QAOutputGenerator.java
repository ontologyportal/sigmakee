package com.articulate.sigma.semRewrite;/*
Copyright 2014-2015 IPsoft

Author: Peigen You Peigen.You@ipsoft.com

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

import com.articulate.sigma.Document;
import com.articulate.sigma.Formula;
import com.articulate.sigma.KBmanager;
import com.google.common.collect.Lists;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class QAOutputGenerator {

    /************************************************************
     */
    public static ArrayList<Path> getAllFilenamesInDir() throws IOException {

        ArrayList<Path> res = new ArrayList<Path>();
        Files.walk(Paths.get("/Users/peigenyou/Downloads")).forEach(filePath -> {
            if (Files.isRegularFile(filePath)) {
                System.out.println(filePath);
                res.add(filePath);
            }
        });
        return res;
    }

    /************************************************************
     */
    public static ArrayList<ArrayList<String>> extractFile(Path p) {

        ArrayList<String> querys = new ArrayList<String>();
        ArrayList<String> anses = new ArrayList<String>();
        ArrayList<ArrayList<String>> res = new ArrayList<ArrayList<String>>();

        try (Scanner in = new Scanner(p)) {
            while (in.hasNextLine()) {
                String input = in.nextLine();
                input = input.replaceAll("[\"]", "\\\"");
                //passage
                if (input.startsWith("&&")) {
                    try (PrintWriter out = new PrintWriter(p.toString() + ".passage")) {
                        input = in.nextLine();
                        while (input != null && !input.startsWith("&&")) {
                            out.append(input);
                            input = in.nextLine();
                        }
                        out.flush();
                    }
                    while (in.hasNext()) {
                        input = in.nextLine();
                        input = input.replaceAll("\"", "\\\"");
                        if (input.startsWith("@@")) {
                            input = input.substring(2);
                            querys.add(input);
                        }
                        if (input.startsWith("!!")) {
                            input = input.substring(2);
                            anses.add(input);
                        }
                        if (input.startsWith("??")) {
                            input = "not correct";
                            anses.add(input);
                        }
                    }
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        res.add(querys);
        res.add(anses);
        return res;
    }

    /************************************************************
     * Generate output from an array of strings
     */
    public static List<Record> getRecordFromStringSet(String[] strs, Interpreter inter) {

        ArrayList<Record> res = new ArrayList<Record>();
        Field questionfield = null;
        try {
            questionfield = inter.getClass().getDeclaredField("question");
            questionfield.setAccessible(true);
            Field userInputsfield = inter.getClass().getDeclaredField("userInputs");
            userInputsfield.setAccessible(true);
            Document userInputs = (Document) userInputsfield.get(inter);
            int i = 0;
            for (String s : strs) {
                Record r = new Record(i++, s);
                s = s.trim();
                if (s.endsWith("?"))
                    questionfield.setBoolean(inter, true);
                else
                    questionfield.setBoolean(inter, false);

                if (!questionfield.getBoolean(inter)) {
                    inter.tfidf.addInput(s);
                }
                ArrayList<String> kifClauses;
                try {
                    ArrayList<CNF> inputs = Lists.newArrayList(inter.interpretGenCNF(s));
                    r.CNF = inputs.get(0);
                    kifClauses = inter.interpretCNF(inputs);
                }
                catch (Exception e) {
                    System.out.println("Paring error in sentence :" + s);
                    r.CNF = new CNF();
                    r.result = "Paring error";
                    e.printStackTrace();
                    continue;
                }
                String s1 = inter.toFOL(kifClauses);
                String s2 = inter.postProcess(s1);
                String s3 = inter.addQuantification(s2);
                r.KIF = new Formula(s3).toString();

                String result = inter.fromKIFClauses(kifClauses);
                System.out.println("INFO in Interpreter.interpretSingle(): Theorem proving result: '" + result + "'");

                if (questionfield.getBoolean(inter)) {
                    if (("I don't know.".equals(result) && inter.autoir) || inter.ir) {
                        if (inter.autoir) {
                            System.out.println("Interpreter had no response so trying TFIDF");
                        }
                        result = inter.tfidf.matchInput(s).toString();
                    }
                }
                else {
                    // Store processed sentence
                    userInputs.add(s);
                }

                //System.out.println("INFO in Interpreter.interpretSingle(): combined result: " + result);
                r.result = result;
                res.add(r);
            }
        }
        catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        catch (NoSuchFieldException e) {
            e.printStackTrace();
        }

        return res;
    }

    /************************************************************
     * save the record to a json file
     */
    public static void saveRecord(List<Record> list, String path) throws FileNotFoundException {

        if (list == null) {
            System.out.println("INFO in RewriteRuleUtil.saveRecord():  list is null");
            return;
        }
        try (PrintWriter pw = new PrintWriter(path)) {
            JSONArray arr = new JSONArray();
            for (Record k : list) {
                JSONObject obj = new JSONObject();
                obj.put("index", k.index + "");
                obj.put("sentence", k.sen);
                obj.put("CNF", k.CNF.toString());
                obj.put("KIF", k.KIF);
                obj.put("result", k.result);
                arr.add(obj);
            }
            pw.println(arr.toJSONString());
            pw.flush();
        }
    }

    /************************************************************
     * class to store output
     */
    public static class Record {

        public int index;
        public String sen;
        public CNF CNF;
        public String KIF;
        public String result;

        public Record(int index, String sen) {

            this.index = index;
            this.sen = sen;
        }

        @Override
        public String toString() {

            StringBuilder sb = new StringBuilder();
            sb.append("{\n");
            sb.append("  \"index\":\"" + index + "\",\n");
            sb.append("  \"sentence\":\"" + sen + "\",\n");
            sb.append("  \"CNF\":\"" + CNF + "\",\n");
            sb.append("  \"KIF\":\"" + KIF + "\",\n");
            sb.append("  \"result\":\"" + result + "\"\n");
            sb.append("}\n");
            return sb.toString();
        }
    }

    /************************************************************
     * generate output with filename and output to jasonfilename
     * both should with path
     */
    public static void generate(String inputPath, String outputPath) {

        KBmanager.getMgr().initializeOnce();
        Interpreter inter = new Interpreter();
        inter.initialize();
        String[] strs = CommonCNFUtil.loadSentencesFromTxt(inputPath);
        System.out.println("Strins are: " + strs);
        List<Record> res = null;
        res = getRecordFromStringSet(strs, inter);
        System.out.println(res);
        try {
            saveRecord(res, outputPath);
        }
        catch (FileNotFoundException e) {
            System.out.println("Can't save file.");
            e.printStackTrace();
            return;
        }
    }

    /************************************************************
     */
//    public static void testOnOnePassage() {
//
//        String path = "/Users/peigenyou/workspace/input.txt";
//        String outputPath = "/Users/peigenyou/workspace/output.json";
//        generate(path, outputPath);
//    }
//
//    /************************************************************
//     */
//    public static void testExtractFile() {
//
//        try {
//            ArrayList<Path> paths = getAllFilenamesInDir();
//            StringBuilder sb = new StringBuilder();
//            for (Path p : paths) {
//                ArrayList<ArrayList<String>> qa = extractFile(p);
//                for (int i = 0; i < qa.get(0).size(); ++i) {
//                    sb.append("{\n");
//                    sb.append("  \"file\":\"" + p.getFileName() + "\",\n");
//                    sb.append("  \"query\":\"" + qa.get(0).get(i) + "\",\n");
//                    sb.append("  \"answer\":\"" + qa.get(1).get(i) + "\"\n");
//                    sb.append("},\n");
//                }
//            }
//            try (PrintWriter out = new PrintWriter("/Users/peigenyou/Downloads/qa.json")) {
//                out.print(sb.toString());
//            }
//
//        }
//        catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    public static void main(String[] args) {

//        testOnOnePassage();

    }

}
