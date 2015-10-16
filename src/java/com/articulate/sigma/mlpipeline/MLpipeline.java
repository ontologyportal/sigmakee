package com.articulate.sigma.mlpipeline;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Created by apease on 10/1/15.
 */
public class MLpipeline {

    public class Data {

        String question = null;
        ArrayList<String> answers = null;
        ArrayList<Map<String,String>> tokens = new ArrayList<>();
    }

    public void readPreambles() { // vocab, stopwords etc
        // read vocab
        // read stopwords
    }

    public void readTraining() {  // read and generate features from training file

    }

    // read a file of features
    public void processHeaderLine(String s) { // put column names and types into collection

    }

    public void processDataLine(String s) { // put column names and types into collection, assuming conformance to header order

    }

    public void pipeline() {

        readPreambles();
        readTraining();
        try {
            Files.lines(Paths.get(".", "data.txt"))
                    .forEach(s -> {if (s.startsWith("#")) processHeaderLine(s); else processDataLine(s); return;});
        }
        catch (IOException ioe) {
            System.out.println("Error in MLpipeline.pipeline()");
            System.out.println(ioe.getMessage());
            ioe.printStackTrace();
        }
    }

    public static void main(String[] args) {

    }
}
