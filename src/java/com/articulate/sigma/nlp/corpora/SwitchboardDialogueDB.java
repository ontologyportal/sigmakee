package com.articulate.sigma.nlp.corpora;

import com.opencsv.CSVReader;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.lang.String;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.*;

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
 * Created by Vishwas Mruthyunjaya on 1/4/17.
 */
public class SwitchboardDialogueDB {

    // Global Properties for SwitchDialogueDB Object.
    private String directoryName = "";
    private Path addToDirectory;
    private List<String> foldersList = new ArrayList<>();
    private List<String> filesList = new ArrayList<>();

    /************************************************************************************
     * This method takes the converted data and
     * stores in a text file format which is required for the ChatBot.
     */
    private void writeFile(List<String> rawData, String fileName, String folderName) {

        String addToFileName;
        String addToFolderName;
        String addToFilePath;
        Path addToFolderPath;
        try {
            addToFileName = StringUtils.substringBefore(FilenameUtils.getBaseName(fileName), ".");
            addToFolderName = folderName.substring(folderName.lastIndexOf("/") + 1);
            addToFolderPath = Paths.get(addToDirectory+"/"+addToFolderName+"_parsed/");
            Files.createDirectories(addToFolderPath);
            addToFilePath = addToFolderPath+"/"+addToFileName+"_parsed.txt";
            Path file = Paths.get(addToFilePath);
            Files.write(file, rawData, Charset.forName("UTF-8"));
        }
        catch (Exception ex) {
            System.out.println (ex.toString());
            System.out.println("writeFile() Method: Could not find the file");
        }
    }

    /************************************************************************************
     * This method is to read the file(s) from Switchboard Dialogue Database and extract
     * the necessary column as well as convert them into required one linguistic turn format.
     */
    private void readFile(String filePath, String folderPath) {

        // Initialise parameters
        String [] data;
        StringBuilder buf = new StringBuilder();
        List<String> rawData = new ArrayList<>();
        // Identified conversation column and conversing agents column in the CSV file
        int conversationColumnIndex = 8;
        int conversingAgentsColumnIndex = 7;

        // CSV file reading and formatting
        try {
            // CSV File reader
            CSVReader reader = new CSVReader(new FileReader(filePath));
            // This skips the header row in CSV file
            reader.readNext();
            // Formatting the CSV file data to required style
            while ((data = reader.readNext()) != null) {
                // data[] is an array of values from the line
                data[conversationColumnIndex] = data[conversationColumnIndex].replaceAll("[{][a-zA-Z0-9]","");
                data[conversationColumnIndex] = data[conversationColumnIndex].replaceAll("[{\\-}\\[\\]\"/+()]","");
                data[conversationColumnIndex] = data[conversationColumnIndex].replaceAll("( )+"," ");
                if (Integer.parseInt(data[conversingAgentsColumnIndex]) == 1 && buf.length() > 0) {
                    rawData.add(buf.toString().trim());
                    buf.setLength(0);
                }
                buf.append(data[conversationColumnIndex].trim());
                buf.append(" ");
            }
            if (buf.length() > 0 ) rawData.add(buf.toString().trim());
            // Invoke writeFile method to write the converted data into a .txt file
            writeFile(rawData, filePath, folderPath);
        }
        catch (IOException ex) {
            System.out.println (ex.toString());
            System.out.println("Warning: In method readFile(), could not find the file");
        }
        catch (Exception e) {
            System.out.println("Warning: In method readFile(), error in data formatting");
        }
    }

    /*************************************************************************************
     * listFolders() method takes a directory path as argument and lists all the folder
     * under the given folder path.
     * RETURN --> list of absolute folder paths.
     */
    private List<String> listFolders(String directoryPath) {

        List<String> foldersPath = new ArrayList<>();
        try {
            File directory = new File(directoryPath);
            //get all the folders from a directory
            File[] folderList = directory.listFiles();
            if (folderList != null) {
                for (File file : folderList) {
                    if (file.isDirectory()) {
                        foldersPath.add(file.getAbsolutePath());
                    }
                }
            }
        }
        catch (Exception e) {
            System.out.println("Warning: In method listFolders(), error in listing folders under: " + directoryPath);
        }
        return foldersPath;
    }

    /*************************************************************************************
     * listFiles() method takes a folder path as argument and lists all the files under
     * the given folder path.
     * RETURN --> list of absolute file paths.
     */
    private List<String> listFiles(String folderPath) {
        
        List<String> filesPath = new ArrayList<>();
        try {
            File directory = new File(folderPath);
            //get all the files from a folder
            File[] filesList = directory.listFiles();
            if (filesList != null) {
                for (File file : filesList) {
                    if (file.isFile() && FilenameUtils.getExtension(file.toString()).equalsIgnoreCase("csv")) {
                        filesPath.add(file.getAbsolutePath());
                    }
                }
            }
        }
        catch (Exception e) {
            System.out.println("Warning: In method listFiles(), error in listing the files under: " + folderPath);
        }
        return filesPath;
    }

    /**************************************************************************************
     * main() instantiates class and runs functionality
     */
    public static void main(String[] args) throws IOException {

        SwitchboardDialogueDB switchboardDialogueDBObject = new SwitchboardDialogueDB();
        Properties prop = new Properties();

        try {
            InputStream input = new FileInputStream("corpora.properties");
            prop.load(input);
        }
        catch (IOException e) {
            System.out.println("Problem loading resource file " + e);
            e.printStackTrace();
        }

        switchboardDialogueDBObject.directoryName = prop.getProperty("switchboardDialogueDirectoryName");
        switchboardDialogueDBObject.addToDirectory = Paths.get(prop.getProperty("switchboardDialogueParsedDirectoryName"));

        Files.createDirectories(switchboardDialogueDBObject.addToDirectory);
        switchboardDialogueDBObject.foldersList = switchboardDialogueDBObject.listFolders(switchboardDialogueDBObject.directoryName);
        for (String folderPath: switchboardDialogueDBObject.foldersList) {
            switchboardDialogueDBObject.filesList = switchboardDialogueDBObject.listFiles(folderPath);
            for (String filePath: switchboardDialogueDBObject.filesList) {
                switchboardDialogueDBObject.readFile(filePath, folderPath);
            }
        }

    }
}
