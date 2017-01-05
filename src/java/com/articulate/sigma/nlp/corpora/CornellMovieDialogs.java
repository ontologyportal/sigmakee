package com.articulate.sigma.nlp.corpora;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by charlescostello on 1/4/17.
 * Class to parse Cornell Movie Dialog file and write to new file
 * Data source: cs.cornell.edu/~cristian/Cornell_Movie-Dialogs_Corpus.html
 */
public class CornellMovieDialogs {

    private String rawFileName = "/Users/charlescostello/CloudMinds/data/cornellMovieDialogs/movie_lines.txt";
    private String parsedFileName = "/Users/charlescostello/CloudMinds/data/cornellMovieDialogs/movie_lines_parsed.txt";

    /****************************************************************
     * @param parsedLines are the parsed dialog lines
     */
    private void writeFile(List<String> parsedLines, String fileName) {
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(fileName))) {
            for (String parsedLine: parsedLines) {
                bufferedWriter.write(parsedLine);
                bufferedWriter.newLine();
            }
        } catch (IOException e) {
            System.out.println("Error with" + fileName + ": " + e);
            e.printStackTrace();
        }
    }

    /****************************************************************
     * @param rawLines are the raw dialog lines
     * @return a list of parsed dialog lines
     */
    private List<String> parseLines(List<String> rawLines) {
        List<String> parsedLines = new ArrayList<>();

        for (String line: rawLines) {
            String[] splitString = line.split("\\+\\+\\+\\$\\+\\+\\+");
            parsedLines.add(splitString[splitString.length - 1].trim());
        }

        return parsedLines;
    }


    /****************************************************************
     * @param fileName is the name of Cornell Movie Dialog file
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
        CornellMovieDialogs cornellMovieDialogs = new CornellMovieDialogs();

        // Read file and convert to list of raw lines
        List<String> rawLines = cornellMovieDialogs.readFile(cornellMovieDialogs.rawFileName);

        // Parse raw lines
        List<String> parsedLines = cornellMovieDialogs.parseLines(rawLines);

        // Write parsed lines to new file
        cornellMovieDialogs.writeFile(parsedLines, cornellMovieDialogs.parsedFileName);
    }
}