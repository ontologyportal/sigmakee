package com.articulate.sigma.nlp.corpora;

import org.apache.commons.io.FilenameUtils;

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
 * Class to parse Ubuntu dialog files and write to new files
 * Data source: github.com/rkadlec/ubuntu-ranking-dataset-creator
 */
public class UbuntuDialogs {


    private String rawDirectoryName;
    private String parsedDirectoryName;

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
     * @param fileName is the name of Ubuntu dialog file
     * @return a list of raw dialog lines
     * Reads dialog file contents into list of lines
     */
    private List<String> parseFile(String fileName) {

        List<String> lines = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        String currentUser = "";

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(fileName))) {
            String line;

            while ((line = bufferedReader.readLine()) != null) {
                // Split string on tab and add dialog to list, alternating based on the user writing dialog
                // One linguistic term per line
                String[] splitString = line.split("\t");
                if (splitString.length == 4) {
                    if (!splitString[1].equals(currentUser)) {
                        if (buffer.length() > 0) lines.add(buffer.toString().trim());
                        buffer.setLength(0);
                        currentUser = splitString[1];
                    }

                    buffer.append(splitString[3].trim());
                    buffer.append(" ");
                }
            }

            if (buffer.length() > 0) lines.add(buffer.toString().trim());
        }
        catch (IOException e) {
            System.out.println("Error with" + fileName + ": " + e);
            e.printStackTrace();
        }

        return lines;
    }

    /****************************************************************
     * Iterates through all files and runs file specific functionality
     */
    private void parseAllFiles() {

        File topLevelDirectory = new File(rawDirectoryName);
        File[] directories = topLevelDirectory.listFiles();

        if (directories != null) {
            for (File directory: directories) {
                File[] files = directory.listFiles();

                if (files != null) {
                    // Create new directory for parsed files
                    File parsedDirectory = new File(parsedDirectoryName + directory.getName() + "/");
                    parsedDirectory.mkdir();

                    // Parse and write each file
                    for (File file: files) {
                        List<String> parsedLines = parseFile(file.getAbsolutePath());
                        String parsedFilePath = parsedDirectoryName + directory.getName() + "/" + FilenameUtils.getBaseName(file.getAbsolutePath()) + "_parsed.txt";
                        writeFile(parsedLines, parsedFilePath);
                    }
                }
            }
        }
    }

    /****************************************************************
     * @param args command line arguments
     * Instantiates class and runs functionality
     */
    public static void main(String[] args) {

        // Instantiate class
        UbuntuDialogs ubuntuDialogs = new UbuntuDialogs();

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
        ubuntuDialogs.rawDirectoryName = prop.getProperty("ubuntuRawDirectoryName");
        ubuntuDialogs.parsedDirectoryName = prop.getProperty("ubuntuParsedDirectoryName");

        // Run functionality
        ubuntuDialogs.parseAllFiles();
    }
}