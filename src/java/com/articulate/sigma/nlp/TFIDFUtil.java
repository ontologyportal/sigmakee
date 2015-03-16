package com.articulate.sigma.nlp;

import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.util.StringUtils;

import java.io.*;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

/**
 * Created by areed on 3/4/15.
 */
public class TFIDFUtil {


    /** ***************************************************************
     * This method reads in a text file, breaking it into single line documents
     * Currently, sentences are not separated if they occur on the same line.
     * @param filename file to be read
     * @param separateSentences should sentences be separated if they occur on one line
     * @return list of strings from each line of the document
     */
    public static List<String> readFile(String filename, boolean separateSentences) throws IOException {
        List<String> documents = Lists.newArrayList();
        URL fileURL = Resources.getResource(filename);
        File f = new File(filename);
        BufferedReader bf = new BufferedReader(new FileReader(fileURL.getPath()));
        String line;
        while ((line = bf.readLine()) != null) {
            if (line == null || line.equals("")) continue;

            documents.add(line);
        }
        return documents;
    }
}
