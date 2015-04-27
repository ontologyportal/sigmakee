package com.articulate.sigma.semRewrite;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.function.Function;

import com.articulate.sigma.KBmanager;
import com.articulate.sigma.test.JsonReader;

import org.json.simple.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.articulate.sigma.semRewrite.datesandnumber.InterpretNumerics;


@RunWith(Parameterized.class)
public class DurationTest {

	@Parameterized.Parameter(value= 0)
	public String fInput;
	@Parameterized.Parameter(value= 1)
	public String fExpected;   

	 /** ***************************************************************
     */
    @BeforeClass
    public static void initInterpreter() {
        KBmanager.getMgr().initializeOnce();
    }
    /** ***************************************************************
     */
	@Parameters(name="{0}")
	public static Collection<String[]> prepare() {
  
		return JsonReader.transform("resources/Duration_test.json", new Function<JSONObject, String[]>() {
			@Override
			public String[] apply(JSONObject jo) {
			String text = (String) jo.get("text");
			String kif = (String) jo.get("kif");
			return new String[]{text, kif};
			}
			});
	}
	 /** ***************************************************************
     */
	@Test
	public void test() {
		System.out.println("Running date number tests for sentence : "+fInput);
		String fout = InterpretNumerics.getSumoTerms(fInput).toString();
		System.out.println(fout);
		System.out.println(fExpected);
		assertEquals(fExpected, fout);
	}
}