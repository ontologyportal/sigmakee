package com.articulate.sigma.semRewrite;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ListIterator;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.articulate.sigma.IntegrationTestBase;
import com.articulate.sigma.semRewrite.datesandnumber.InterpretNumerics;

@RunWith(Parameterized.class)
public class DateAndNumberGenerationTest {

	public static InterpretNumerics interpNum;

	@Parameterized.Parameter(value= 0)
	public String fInput;
	@Parameterized.Parameter(value= 1)
	public String fExpected;   

	@Parameters(name="{0}")
	public static Collection<String[]> prepare() {

		ArrayList<String[]> result = new ArrayList<String[]>();
		File jsonTestFile = new File(IntegrationTestBase.RESOURCES_FILE, "Date_and_number_test.json");
		String filename = jsonTestFile.getAbsolutePath();
		JSONParser parser = new JSONParser();  
		try {  
			Object obj = parser.parse(new FileReader(filename));  
			JSONArray jsonArray = (JSONArray) obj; 
			ListIterator<JSONObject> li = jsonArray.listIterator();
			while (li.hasNext()) {
				JSONObject jo = li.next();
				String text = (String) jo.get("text");
				String kif = (String) jo.get("kif");
				result.add(new String[]{text,kif});
			}			 
		} 
		catch (FileNotFoundException e) {  
			e.printStackTrace();  
		} 
		catch (IOException e) {  
			e.printStackTrace();  
		} 
		catch (ParseException e) {  
			e.printStackTrace();  
		} 	
		System.out.println(result);
		return result;    
	}

	@Test
	public void test() {
		System.out.println("Running date number tests for sentence : "+fInput);
		assertEquals(fExpected, InterpretNumerics.getSumoTerms(fInput).toString());
	}

}
