package com.articulate.sigma.nlp.corpora;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by charlescostello on 1/4/17.
 * Class to parse Ubuntu dialog files and write to new files
 * Data source: github.com/rkadlec/ubuntu-ranking-dataset-creator
 */
public class UbuntuDialogs {

    private String rawFileName = "/Users/charlescostello/CloudMinds/data/ubuntuDialogs/521/1.tsv";
    private String parsedFileName = "/Users/charlescostello/CloudMinds/data/ubuntuDialogs/521/1_parsed.txt";

    /****************************************************************
     * @param parsedLines are the parsed dialog lines
     */
    private void writeFile(List<String> parsedLines, String fileName) {

    }

    /****************************************************************
     * @param rawLines are the raw dialog lines
     * @return a list of parsed dialog lines
     */
    private List<String> parseLines(List<String> rawLines) {
        List<String> parsedLines = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        String currentUser = "";

        for (String line: rawLines) {
            String[] splitString = line.split("\t");

            if (!splitString[1].equals(currentUser)) {
                if (buffer.length() > 0) parsedLines.add(buffer.toString().trim());
                buffer.setLength(0);
                currentUser = splitString[1];
            }

            buffer.append(splitString[3].trim());
            buffer.append(" ");
        }

        return parsedLines;
    }


    /****************************************************************
     * @param fileName is the name of Ubuntu dialog file
     * @return a list of raw dialog lines
     */
    private List<String> readFile(String fileName) {
        List<String> rawLines = new ArrayList<>();

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                rawLines.add(line);
            }
        } catch (IOException e) {
            System.out.println("Error with" + fileName + ": " + e);
            e.printStackTrace();
        }

        return rawLines;
    }

    /****************************************************************
     * @param args command line arguments
     */
    public static void main(String[] args) {
        UbuntuDialogs ubuntuDialogs = new UbuntuDialogs();

        // Read file and convert to list of raw lines
        List<String> rawLines = ubuntuDialogs.readFile(ubuntuDialogs.rawFileName);

        // Parse raw lines
        List<String> parsedLines = ubuntuDialogs.parseLines(rawLines);

        parsedLines.forEach(System.out::println);

//        for (int i = 0; i < rawLines.size(); i++) {
//            System.out.println(rawLines.get(i));
//            System.out.println(parsedLines.get(i));
//            System.out.println();
//        }

        // Write parsed lines to new file
//        ubuntuDialogs.writeFile(parsedLines, ubuntuDialogs.parsedFileName);
    }
}
