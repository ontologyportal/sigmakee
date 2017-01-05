package com.articulate.sigma.nlp.corpora;

import com.opencsv.CSVReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.String;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Vishwas Mruthyunjaya on 1/4/17.
 */
public class SwitchboardDialogueDB {
    /**
     * This method takes the converted data and
     * stores in a text file format which is required for the ChatBot.
     */
    public void writeFile(List<String> rawData){
        try{
            Path file = Paths.get("/home/vish/parse1.txt");
            Files.write(file, rawData, Charset.forName("UTF-8"));
        }
        catch (IOException ex){
            System.out.println (ex.toString());
            System.out.println("Could not find the file");
        }
    }


    /**
     * This method is to read the file(s) from Switchboard Dialogue Database and
     * extract the necessary column as well as convert them into required format.
     */
    public void readFile(String fName){
        // Initiate parameters
        String [] text;
        StringBuffer buf = new StringBuffer();
        List<String> rawData = new ArrayList<>();
        // CSV file reading and formatting
        try{
            // CSV File reader
            CSVReader reader = new CSVReader(new FileReader(fName));
            reader.readNext();
            // Formatting the CSV file data to required style
            while ((text = reader.readNext()) != null) {
                // text[] is an array of values from the line
                text[8] = text[8].replaceAll("[{][a-zA-Z0-9]","");
                text[8] = text[8].replaceAll("[{\\-}\\[\\]\"/+]","");
                text[8] = text[8].replaceAll("( )+"," ");
                if (Integer.parseInt(text[7]) == 1 && buf.length() > 0){
                    rawData.add(buf.toString().trim());
                    buf.setLength(0);
                }
                buf.append(text[8].trim());
                buf.append(" ");
            }
            // Invoke writeFile method to write the converted data into a .txt file
            writeFile(rawData);
        }
        catch (IOException ex){
            System.out.println (ex.toString());
            System.out.println("Could not find the file");
        }
        catch(Exception e){
            System.out.println("Warning: Exception in data formatting");
        }

    }


    /**
     * Main()
     */
    public static void main(String[] args) throws IOException {
        SwitchboardDialogueDB swdaObj = new SwitchboardDialogueDB();
        swdaObj.readFile("/home/vish/Downloads/swda/swda/sw00utt/sw_0001_4325.utt.csv");
    }

}
