package com.articulate.sigma;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class FileUtils {
	
	/**
	 * Get the file reader.
	 * @param fileName
	 * @return
	 * @throws FileNotFoundException
	 */
	public static BufferedReader getReader(String fileName)throws IOException{
		InputStream is = new FileInputStream(fileName);
		BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
		return reader;
	}
	/**
	 * Get file writer.
	 * @param fileName
	 * @return
	 * @throws IOException
	 */
	public static PrintWriter getWriter(String fileName) throws IOException {
		PrintWriter writer = new PrintWriter(new OutputStreamWriter(
				new FileOutputStream(fileName, true), "UTF-8"));
		return writer;
	}
	/**
	 * Get text from file.
	 * @param file
	 * @return
	 * @throws FileNotFoundException
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 */
	public static String getTextFromFile(File file) throws FileNotFoundException, UnsupportedEncodingException, IOException{
		InputStream is = new FileInputStream(file);
		BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
		String line = "", text = "";
		while((line = reader.readLine()) != null){
			text += line + " ";
		}
		
		reader.close();
		return text.trim();
	}
}
