package com.articulate.sigma;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.*;

public class NER {

    /** *************************************************************
     */
    public static ArrayList<String> extractEntities(String infile) throws IOException {
        
        ArrayList<String> result = new ArrayList<String>();
        Process _nlp;
        BufferedReader _reader; 
        BufferedWriter _writer; 
        BufferedReader _error;
        String stanfordCore = "/home/apease/Programs/stanford-ner-2014-10-26";
        String newcore = KBmanager.getMgr().getPref("stanford-ner");
        if (!StringUtil.emptyString(newcore))
        	stanfordCore = newcore;
        String execString = "/home/apease/Programs/java/jdk1.8.0_25/bin/java -mx700m " +
                "-classpath " + stanfordCore + "/stanford-ner.jar " +
                "edu.stanford.nlp.ie.crf.CRFClassifier " +
                //                 "-loadClassifier  /home/apease/Programs/stanford-ner-2014-10-26/classifiers/english.all.3class.distsim.crf.ser.gz " +
                //                 "-loadClassifier  /home/apease/Programs/stanford-ner-2014-10-26/classifiers/english.nowiki.3class.distsim.crf.ser.gz " +
                //                 "-loadClassifier  /home/apease/Programs/stanford-ner-2014-10-26/classifiers/english.conll.4class.distsim.crf.ser.gz " +
                "-loadClassifier " + stanfordCore + "/classifiers/english.muc.7class.distsim.crf.ser.gz " +
                "-textFile " + infile;
        System.out.println("INFO in NER.extractEntities(): executing: " + execString);
        _nlp = Runtime.getRuntime().exec(execString);
        _reader = new BufferedReader(new InputStreamReader(_nlp.getInputStream()));
        _error = new BufferedReader(new InputStreamReader(_nlp.getErrorStream()));
        //System.out.println("INFO in NER.extractEntities(): initializing process");
        String line = null; 
        while (true) {
            line = _reader.readLine(); 
            System.out.println(line);
            if (line == null)
                break;
            result.add(line);           
        }
        _writer = new BufferedWriter(new OutputStreamWriter(_nlp.getOutputStream()));
        
        return result;
    }

    /** *************************************************************
     */
    public static HashSet<String> processExtraction (ArrayList<String> lines) {
        
        HashSet<String> result = new HashSet<String>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String[] elements = line.split(" ");
            StringBuffer sb = new StringBuffer();
            String lastType = "O";
            for (int j = 0; j < elements.length; j++) {
                String type = elements[j].substring(elements[j].indexOf('/') + 1);
                String word = elements[j].substring(0,elements[j].indexOf('/'));
                System.out.println(word + "-" + type);
                if (!type.equals("O")) {
                    if (!type.equals(lastType)) {
                        if (!lastType.equals("O"))
                            result.add(sb.toString());
                        sb = new StringBuffer();
                        sb.append(word);
                    }
                    else 
                        sb.append("_" + word);
                }
                else {
                    if (!lastType.equals("O")) {
                        String SUMOclass = "";
                        if (lastType.equals("LOCATION"))
                            SUMOclass = "GeographicalArea";
                        if (lastType.equals("ORGANIZATION"))
                            SUMOclass = "Organization";
                        if (lastType.equals("PERSON"))
                            SUMOclass = "Human";
                        result.add(sb.toString() + ":" + SUMOclass);                        
                    }

                }
                lastType = type;
            }
        }
        return result;
    }
    
    /** *************************************************************
     */
    public static void main(String[] args) {

        if (args != null && args.length > 1 && args[0].equals("-f")) {
            try {
                ArrayList<String> entities = extractEntities(args[1]);
                System.out.println(processExtraction(entities));
            }
            catch (Exception ex) {
                System.out.println(ex.getMessage());
                ex.printStackTrace();
            }
        }
        else if (args != null && args.length > 0 && args[0].equals("-h")) {
            System.out.println("Named Entity Recognition");
            System.out.println("  options:");
            System.out.println("  -h - show this help screen");
            System.out.println("  -f fname - run on one file");
        }
        else {

        }

    }
}
