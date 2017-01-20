package com.articulate.sigma.nlp.corpora;

import org.apache.commons.io.FilenameUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/*
Copyright 2017 Cloudminds Technology, Inc

Author: vishwas.mruthyunjaya@cloudminds.com

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program ; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston,
MA  02111-1307 USA
 */

/****************************************************************************************
 * Created by Vishwas Mruthyunjaya on 1/19/17.
 */
public class NUSSMSCorpus {
    private String directoryName;
    private String writeToDirectory;

    /************************************************************************************
     * This method takes the converted data and
     * stores in a text file format which is required for the ChatBot.
     */
    private void writeFile(List<String> parsedLines, String fileName) {

        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(fileName))) {
            for (String parsedLine : parsedLines) {
                bufferedWriter.write(parsedLine);
                bufferedWriter.newLine();
            }
        }
        catch (IOException e) {
            System.out.println("In Method writeFile(): Error with" + fileName + ": " + e);
            e.printStackTrace();
        }
    }

    /****************************************************************
     * This method is to read the file(s) from NUS SMS Corpus and extract
     * the necessary text from the specific tag.
     */
    private List<String> readFile(String fileName) {

        List<String> rawData = new ArrayList<>();
        DocumentBuilder builder;
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        try {
            builder = builderFactory.newDocumentBuilder();
            Document document = builder.parse(fileName);
            document.getDocumentElement().normalize();
            NodeList posts = document.getElementsByTagName("message");
            for (int i = 0; i < posts.getLength(); i++) {
                Node nNode = posts.item(i);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    rawData.add(eElement.getElementsByTagName("text").item(0).getTextContent().trim());
                }
            }
        }
        catch (ParserConfigurationException  e) {
            System.out.println (e.toString());
            System.out.println("Warning: In method readFile(), error occurred while parsing");
        }
        catch (IOException e) {
            System.out.println (e.toString());
            System.out.println("Warning: In method readFile(), could not find the file");
        }
        catch (SAXException e) {
            System.out.println (e.toString());
            System.out.println("Warning: In method readFile(), exception with builder");
        }
        return rawData;
    }

    /******************************************************************************************************
     * Lists all the files and reads & writes the data in each file listed to a directory
     */
    private void readAllFiles() {

        File directory = new File(directoryName);
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                if (FilenameUtils.getExtension(file.getName()).equals("xml")) {
                    List<String> parsedLines = readFile(file.getAbsolutePath());
                String parsedFilePath = writeToDirectory + "/" + FilenameUtils.getBaseName(file.getAbsolutePath()) + "_parsed.txt";
                writeFile(parsedLines, parsedFilePath);
                }
            }
        }
    }

    /******************************************************************************************************
     * main() instantiates class and runs functionality
     */
    public static void main(String[] args) {

        NUSSMSCorpus nusSMSCorpus = new NUSSMSCorpus();
        Properties prop = new Properties();
        try {
            InputStream input = new FileInputStream("corpora.properties");
            prop.load(input);
        }
        catch (IOException e) {
            System.out.println("Problem loading resource file " + e);
            e.printStackTrace();
        }
        nusSMSCorpus.directoryName = prop.getProperty("nusSMSCorpusDirectoryName");
        nusSMSCorpus.writeToDirectory = prop.getProperty("nusSMSCorpusParsedDirectoryName");
        nusSMSCorpus.readAllFiles();
    }
}
