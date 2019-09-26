package com.articulate.sigma.utils;

import com.google.common.collect.Lists;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;

public class FileUtil {

    /****************************************************************
     * This method reads in a text file, breaking it into single line documents
     * Currently, sentences are not separated if they occur on the same line.
     *
     * @param filename          file to be read
     * @param separateSentences should sentences be separated if they occur on one line
     * @return list of strings from each line of the document
     */
    public static List<String> readLines(String filename, boolean separateSentences) {

        List<String> documents = Lists.newArrayList();
        File f = new File(filename);
        String line = null;
        try {
            BufferedReader bf = new BufferedReader(new FileReader(f));
            while ((line = bf.readLine()) != null) {
                if (line == null || line.equals(""))
                    continue;
                documents.add(line);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            System.out.println("Unable to read line in file. Last line successfully read was: " + line);
        }
        return documents;
    }
}
