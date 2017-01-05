package com.articulate.sigma.nlp.corpora;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by charlescostello on 1/4/17.
 * Class to parse Ubuntu dialog files and write to new files
 * Data source: http://cs.mcgill.ca/~jpineau/datasets/ubuntu-corpus-1.0/
 */
public class UbuntuDialogs {
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
        return new ArrayList<>();
    }


    /****************************************************************
     * @param fileName is the name of Ubuntu dialog file
     * @return a list of raw dialog lines
     */
    private List<String> readFile(String fileName) {
        return new ArrayList<>();
    }

    /****************************************************************
     * @param args command line arguments
     */
    public static void main(String[] args) {
        // Read file and convert to list of raw lines
        UbuntuDialogs cornellMovieDialogs = new UbuntuDialogs();
        List<String> lines = cornellMovieDialogs.readFile("UbuntuDialogs/movie_lines.txt");

        // Parse raw lines
        List<String> parsedLines = cornellMovieDialogs.parseLines(lines);

        // Write parsed lines to new file
        cornellMovieDialogs.writeFile(parsedLines, "UbuntuDialogs/movie_lines_parsed.txt");
    }
}
