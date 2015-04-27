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
import com.articulate.sigma.KBmanager;


public class InterpretNumerics {

    /** ***************************************************************
	 * Returns a list of SU-KIF statements, each corresponding to a date/time/measure found in the input 
	 * natural language string.
	 * @param input: The natural language string.
     * @param substitutor
     * @return List of SU-KIF statements, each date/time/measures are obtained from parser.
	 */
	public static List<String> getSumoTerms(String input) {
		
		StanfordDateTimeExtractor sde = new StanfordDateTimeExtractor();
		List<Tokens> tokensList = sde.populateParserInfo(input);
		DateAndNumbersGeneration generator = new DateAndNumbersGeneration();
		return generator.generateSumoTerms(tokensList, sde);
	}
	
	
	/** ***************************************************************
	 */
	public static void main(String[] args) {
		KBmanager.getMgr().initializeOnce();
        String input = "John was killed on 8/15/2014 at 3:45 PM.";
        System.out.println(input);
        System.out.println(getSumoTerms(input));
        input = "As of 2012, sweet oranges accounted for approximately 70 percent of citrus production.";
        System.out.println(input);
        System.out.println(getSumoTerms(input));
        input = "The standard goal of sigma is to achieve precision to 4.5 standard deviations above or below the mean.";
        System.out.println(input);
        System.out.println(getSumoTerms(input));
        input = "Taj Mahal attracts some 3000000 people a year for visit.";
        System.out.println(input);
        System.out.println(getSumoTerms(input));
        input = "In 2014, Fiat owned 90% of Ferrari.";
        System.out.println(input);
        System.out.println(getSumoTerms(input));
        input = "John killed Mary on 31 March and also in July 1995 by travelling back in time.";
        System.out.println(input);
        System.out.println(getSumoTerms(input));
    }
}
