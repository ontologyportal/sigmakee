package com.articulate.sigma.nlp.corpora;

import org.apache.commons.io.FilenameUtils;
import org.apache.xerces.dom.DeferredElementImpl;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
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
 * Created by charlescostello on 1/18/17.
 * Class to parse NPS Chat corpus files and write to new files
 * Data source: faculty.nps.edu/cmartell/NPSChat.htm
 */
public class NPSChatCorpus {

    private String rawDirectoryName;
    private String parsedDirectoryName;

    /****************************************************************
     * @param parsedLines are the parsed dialog lines
     * Writes parsed lines to new file
     */
    private void writeFile(List<String> parsedLines, String fileName) {

        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(fileName))) {
            for (String parsedLine : parsedLines) {
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
        System.out.println(fileName);

        DocumentBuilder builder;
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        try {
            builder = builderFactory.newDocumentBuilder();
            Document document = builder.parse(fileName);
            document.getDocumentElement().normalize();
            NodeList posts = document.getElementsByTagName("Post");
            for (int i = 0; i < posts.getLength(); i++) {
                String text = posts.item(i).getFirstChild().getNodeValue();
                int user = Integer.parseInt(((DeferredElementImpl) posts.item(i)).getAttribute("user").split("sUser")[1]);
                System.out.println(user + ": " + text);
            }
        }
        catch (ParserConfigurationException | IOException | SAXException e) {
            e.printStackTrace();
        }


//        StringBuilder buffer = new StringBuilder();
//        String currentUser = "";
//
//        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(fileName))) {
//            String line;
//
//            while ((line = bufferedReader.readLine()) != null) {
//                // Split string on tab and add dialog to list, alternating based on the user writing dialog
//                // One linguistic term per line
//                String[] splitString = line.split("\t");
//                if (splitString.length == 4) {
//                    if (!splitString[1].equals(currentUser)) {
//                        if (buffer.length() > 0) lines.add(buffer.toString().trim());
//                        buffer.setLength(0);
//                        currentUser = splitString[1];
//                    }
//
//                    buffer.append(splitString[3].trim());
//                    buffer.append(" ");
//                }
//            }
//
//            if (buffer.length() > 0) lines.add(buffer.toString().trim());
//        }
//        catch (IOException e) {
//            System.out.println("Error with" + fileName + ": " + e);
//            e.printStackTrace();
//        }

        return lines;
    }

    /****************************************************************
     * Iterates through all files and runs file specific functionality
     */
    private void parseAllFiles() {

        File directory = new File(rawDirectoryName);
        File[] files = directory.listFiles();

        if (files != null) {
            // Parse and write each file
            for (File file : files) {
                if (FilenameUtils.getExtension(file.getName()).equals("xml")) {
                    List<String> parsedLines = parseFile(file.getAbsolutePath());
//                String parsedFilePath = parsedDirectoryName + directory.getName() + "/" + FilenameUtils.getBaseName(file.getAbsolutePath()) + "_parsed.txt";
//                writeFile(parsedLines, parsedFilePath);
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
        NPSChatCorpus npsChatCorpus = new NPSChatCorpus();

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
        npsChatCorpus.rawDirectoryName = prop.getProperty("npsChatDirectoryName");
        npsChatCorpus.parsedDirectoryName = prop.getProperty("npsChatParsedDirectoryName");

        // Run functionality
        npsChatCorpus.parseAllFiles();
    }
}
