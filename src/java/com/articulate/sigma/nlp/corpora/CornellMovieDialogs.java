package com.articulate.sigma.nlp.corpora;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * This code is copyright CloudMinds 2017.
 * This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
 * Users of this code also consent, by use of this code, to credit Articulate Software
 * and Teknowledge in any writings, briefings, publications, presentations, or
 * other representations of any software which incorporates, builds on, or uses this
 * code.  Please cite the following article in any publication with references:
 * Pease, A., (2003). The Sigma Ontology Development Environment,
 * in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
 * August 9, Acapulco, Mexico.
 *
 * Created by charlescostello on 1/4/17.
 * Class to parse Cornell Movie Dialog file and write to new file
 * Data source: cs.cornell.edu/~cristian/Cornell_Movie-Dialogs_Corpus.html
 */
public class CornellMovieDialogs {

    /****************************************************************
     * @param parsedLines are the parsed dialog lines
     * Writes parsed lines to new file
     */
    private void writeFile(List<String> parsedLines, String fileName) {

        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(fileName))) {
            for (String parsedLine: parsedLines) {
                bufferedWriter.write(parsedLine);
                bufferedWriter.newLine();
            }
        }
        catch (IOException e) {
            System.out.println("Error with" + fileName + ": " + e);
            e.printStackTrace();
        }
    }

    /****************************************************************
     * @param rawLines are the raw dialog lines
     * @return a list of parsed dialog lines
     * Parses raw lines by removing non-dialog prefix
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
     * Reads dialog file contents into list of lines
     */
    private List<String> readFile(String fileName) {

        List<String> rawLines = new ArrayList<>();

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                rawLines.add(line);
            }
        }
        catch (IOException e) {
            System.out.println("Error with" + fileName + ": " + e);
            e.printStackTrace();
        }

        return rawLines;
    }

    /****************************************************************
     * @param args command line arguments
     * Instantiates class and runs functionality
     */
    public static void main(String[] args) {

        // Instantiate class
        CornellMovieDialogs cornellMovieDialogs = new CornellMovieDialogs();

        // Get paths from properties file
        Properties prop = new Properties();
        try {
            InputStream input = new FileInputStream("corpora.properties");
            prop.load(input);
        }
        catch (IOException e) {
            System.out.println("Problem loading resource file " + e);
            e.printStackTrace();
        }
        String rawFileName = prop.getProperty("cornellRawFileName");
        String parsedFileName = prop.getProperty("cornellParsedFileName");

        // Read file and convert to list of raw lines
        List<String> rawLines = cornellMovieDialogs.readFile(rawFileName);

        // Parse raw lines
        List<String> parsedLines = cornellMovieDialogs.parseLines(rawLines);

        // Write parsed lines to new file
        cornellMovieDialogs.writeFile(parsedLines, parsedFileName);
    }
}