package com.articulate.sigma.semRewrite.datesandnumber;

import java.util.List;

public class InterpretNumerics {

	public static List<String> getSumoTerms(String input) {
		StanfordDateTimeExtractor sde = new StanfordDateTimeExtractor();
		List<Tokens> tokensList = sde.populateParserInfo(input);
		DateAndNumbersGeneration generator = new DateAndNumbersGeneration();
		return  generator.generateSumoTerms(tokensList, sde.getDependencyList());
	}
	public static void main(String[] args) { 
		String input = "John killed Mary on 31 March and also in July 1995 by travelling back in time.";
		List<String> sumoTerms = getSumoTerms(input);
		boolean start = true;
		for(String s: sumoTerms){
			if(!start){
				System.out.print(",\n");
			}else {
				start = false;
			}
			System.out.print(s);
		}
		
	}

}
