package com.articulate.sigma.semRewrite.datesandnumber;

/*
Copyright 2014-2015 IPsoft

Author: Nagaraj Bhat nagaraj.bhat@ipsoft.com
        Rashmi Rao

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

import java.util.List;

public class InterpretNumerics {

	/** ***************************************************************
	 */
	public static List<String> getSumoTerms(String input) {
		
		StanfordDateTimeExtractor sde = new StanfordDateTimeExtractor();
		List<Tokens> tokensList = sde.populateParserInfo(input);
		DateAndNumbersGeneration generator = new DateAndNumbersGeneration();
		return generator.generateSumoTerms(tokensList, sde.getDependencyList());
	}
	
	/** ***************************************************************
	 */
	public static void testTimeDateExtraction() {
		
		System.out.println("----------------------");
		String input = "John killed Mary on 31 March and also in July 1995 by travelling back in time.";
		System.out.println(input);
		List<String> sumoTerms = getSumoTerms(input);
		System.out.println(sumoTerms);
		
		System.out.println("----------------------");
		input = "Amelia Mary Earhart (July 24, 1897 – July 2, 1937) was an American aviator.";
		System.out.println(input);
		sumoTerms = getSumoTerms(input);
		System.out.println(sumoTerms);
		
		System.out.println("----------------------");
		input = "Earhart vanished over the South Pacific Ocean in July 1937 while trying to fly around the world.";
		System.out.println(input);
		sumoTerms = getSumoTerms(input);
		System.out.println(sumoTerms);
		
		System.out.println("----------------------");
		input = "She was declared dead on January 5, 1939.";
		System.out.println(input);
		sumoTerms = getSumoTerms(input);
		System.out.println(sumoTerms);
		
		System.out.println("----------------------");
		input = "Bob went to work only 5 times in 2003.";
		System.out.println(input);
		sumoTerms = getSumoTerms(input);
		System.out.println(sumoTerms);
	}
	
	/** ***************************************************************
	 */
	public static void main(String[] args) {
		
		testTimeDateExtraction();
	}
}
