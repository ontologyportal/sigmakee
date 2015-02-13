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

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateAndNumbersGeneration {
	
	static enum DateComponent {
		DAY, MONTH, YEAR
	}

	public final List<String> MONTHS = new ArrayList<String>(Arrays.asList("january",
			"february","march","april","may","june","july","august",
			"september","october","november","december"));
	public final List<String> DAYS = new ArrayList<String>(Arrays.asList("monday",
			"tuesday","wednesday","thursday","friday","saturday","sunday"));

	static final Pattern DIGITAL_YEAR_PATTERN = Pattern.compile("^[0-9]{4}$");
	static final Pattern WESTERN_YEAR_PATTERN = Pattern.compile("^([0-9]{1,2})(\\/|\\-|\\.)([0-9]{1,2})(\\/|\\-|\\.)([0-9]{4})$");
	static final Pattern DAY_PATTERN = Pattern.compile("^[0-9]{1,2}$");

	private String rootWord = "";
	private List<String> sumoTerms = new ArrayList<String>();
	private HashMap<Integer,String> dateMap = new HashMap<Integer,String>();

	/** ***************************************************************
	 */
	public DateAndNumbersGeneration() {
	}

	/** ***************************************************************
	 */
	private void processDate(Tokens token, List<String> dependencyList) {
		
		Matcher digitalPatternMatcher = DIGITAL_YEAR_PATTERN.matcher(token.getWord());
		Matcher westernYearMatcher = WESTERN_YEAR_PATTERN.matcher(token.getWord());
		Matcher dayMatcher = DAY_PATTERN.matcher(token.getWord());
		if (MONTHS.contains(token.getWord().toLowerCase())) {
			dateMap.put(token.getId(), "MONTH@"+token.getWord());
		} 
		else if (DAYS.contains(token.getWord().toLowerCase())) {
			sumoTerms.add("(" + "WeekDayFn" + " " + token.getWord() + ")");
		} 
		else if (digitalPatternMatcher.find()) {
			dateMap.put(token.getId(), "YEAR@" + token.getWord());
		} 
		else if (westernYearMatcher.find()) {
			sumoTerms.add("(during" + rootWord + " (" + "DayFn" + " " + westernYearMatcher.group(3) +
					" (" + "MonthFn" + " " + MONTHS.get(Integer.valueOf(westernYearMatcher.group(1))-1)
					+ " (" + "YearFn" + " " + westernYearMatcher.group(5) + ")" + ")" + ")" + ")");
		} 
		else if (dayMatcher.find()) {
			dateMap.put(token.getId(), "DAYS@" + token.getWord());
		}
	}

	/** ***************************************************************
	 */
	private void populateDate(FlagUtilities flags,DateComponent dateComponent, String wordToken, DateInfo dateSet) {
		
		switch(dateComponent) {
			case MONTH : flags.setMonthFlag(true);
				dateSet.setMonth(wordToken);
				break;
			case YEAR : flags.setYearFlag(true);
				dateSet.setYear(wordToken);
				break;
			case DAY : flags.setDayFlag(true);
				dateSet.setDay(wordToken);
				break;
		}
	}

	/** ***************************************************************
	 */
	private List<DateInfo> gatherDateSet() {
		
		FlagUtilities flags = new FlagUtilities();
		DateInfo dateInfo = new DateInfo();
		List<DateInfo> dateList = new ArrayList<DateInfo>();
		DateInfo dateInfoTemp;
		String wordToken;
		Iterator<Map.Entry<Integer, String>> dateEntries = dateMap.entrySet().iterator();
		while (dateEntries.hasNext()){
			
			Map.Entry<Integer, String> dateEntry = dateEntries.next();
			wordToken = dateEntry.getValue().split("@")[1];
			
			if (dateEntry.getValue().contains("MONTH")) {
				if (!flags.isMonthFlag()){
					populateDate(flags, DateComponent.MONTH, wordToken, dateInfo);
				}
				else if (!flags.isDayFlag() && !flags.isYearFlag()) {
					dateInfoTemp = new DateInfo();
					dateInfoTemp.setMonth(wordToken);
					dateList.add(dateInfoTemp);
					flags.setMonthFlag(true);

				}
				else {
					addAndResetFlags(dateInfo, dateList, flags, DateComponent.MONTH, wordToken);
				}
			}
			else if (dateEntry.getValue().contains("DAYS")) {
				if (!flags.isDayFlag()){
					populateDate(flags, DateComponent.DAY, wordToken, dateInfo);
				}
				else if (!flags.isMonthFlag() && !flags.isYearFlag()) {
					dateInfoTemp = new DateInfo();
					dateInfoTemp.setDay(wordToken);
					dateList.add(dateInfoTemp);
					flags.setDayFlag(true);
				}
				else {
					addAndResetFlags(dateInfo, dateList, flags, DateComponent.DAY, wordToken);
				}
			}
			else if (dateEntry.getValue().contains("YEAR")){
				if (!flags.isYearFlag()) {
					populateDate(flags, DateComponent.YEAR, wordToken, dateInfo);
				}
				else if (!flags.isDayFlag() && !flags.isMonthFlag()){
					dateInfoTemp = new DateInfo();
					dateInfoTemp.setYear(wordToken);
					dateList.add(dateInfoTemp);
					flags.setYearFlag(true);
				}
				else {
					addAndResetFlags(dateInfo, dateList, flags, DateComponent.YEAR, wordToken);
				}	
			}
			else if (flags.isYearFlag() && flags.isMonthFlag() && flags.isDayFlag()) {
				dateInfoTemp = new DateInfo(dateInfo);
				dateList.add(dateInfoTemp);
				dateInfo.clear();
				flags.resetFlags();
			}
		}
		dateList.add(dateInfo);
		return dateList;
	}

	/** ***************************************************************
	 */
	private void addAndResetFlags(DateInfo dateSet, List<DateInfo> dateList, FlagUtilities flags, DateComponent dateComponent, String token) {
		
		DateInfo dateSetTemp;
		dateSetTemp = new DateInfo(dateSet);
		dateList.add(dateSetTemp);
		dateSet.clear();
		flags.resetFlags();
		switch (dateComponent) {
		case DAY :
			dateSet.setDay(token);
			flags.setDayFlag(true);
			break;
		case MONTH:
			dateSet.setMonth(token);
			flags.setMonthFlag(true);
			break;
		case YEAR:
			dateSet.setYear(token);
			flags.setYearFlag(true);
		}
	}
	
	/** ***************************************************************
	 */
	private void generateSumoDateTerms(){

		List<DateInfo> dateList = gatherDateSet();
		for (DateInfo date : dateList) {
			StringBuffer dateFn = new StringBuffer();
			if (date.getDay() != null) {
				dateFn.append(" (" + "DayFn" + " " + date.getDay());
			}
			if (date.getMonth() != null) {
				dateFn.append(" (" + "MonthFn" + " " + date.getMonth());
			}
			if (date.getYear() != null) {
				dateFn.append(" (" + "YearFn" + " " + date.getYear());
			}
			if (dateFn.length() == 0)
				return;
			int charCount = dateFn.toString().replaceAll("[^(]", "").length();
			for (int i = 0; i <charCount; ++i) {
				dateFn.append(")");
			}
			sumoTerms.add("(during" + rootWord + dateFn.toString());
		}
	}

	/** ***************************************************************
	 */
	private void processNumber(Tokens token, List<String> dependencyList) {
		
		List<String> numDependencyList = new ArrayList<String>();
		for (String dependency : dependencyList) {
			if (dependency.contains(token.getWord() + "-" + token.getId())) {
				numDependencyList.add(dependency);
			}
		}
		if (!numDependencyList.isEmpty()) {
			for (String numDependencyStr : numDependencyList) {
				System.out.println("INFO in DateAndNumbersGeneration.processNumber(): " + numDependencyStr);
				numDependencyStr = numDependencyStr.replace("num(", "").replace(")", "");
				numDependencyStr = numDependencyStr.replace("number(", "").replace(")", "");
				String[] tokenizedWords = numDependencyStr.split(", ");			

				int firstTokenId = Integer.valueOf(tokenizedWords[0].split("-")[1]);
				String firstTokenStr = tokenizedWords[0].split("-")[0];

				int secondTokenId = Integer.valueOf(tokenizedWords[1].split("-")[1]);
				String secondTokenStr = tokenizedWords[1].split("-")[0];

				if (token.getId() == firstTokenId ) {
					sumoTerms.add("(" + "MeasureFn " + token.getWord() + " " + secondTokenStr + ")");
				} 
				else if(token.getId() == secondTokenId) {
					sumoTerms.add("(" + "MeasureFn " + token.getWord() + " " + firstTokenStr + ")");
				}
			}
		}
	}
	
	/** ***************************************************************
	 */
	private void setRootWord(List<String> dependencyList) {
		
		for (String dependencyStr : dependencyList) {
			if (dependencyStr.contains("root(ROOT-0,")) {
				dependencyStr = dependencyStr.replace("root(ROOT-0,", "").replace(")", "");
				rootWord = dependencyStr;
			}
		}
	}

	/** ***************************************************************
	 */
	public List<String> generateSumoTerms(List<Tokens> tokensList, List<String> dependencyList) {
		
		setRootWord(dependencyList);
		for(Tokens token : tokensList) {
			switch(token.getNer()) {
			case "DATE"  : processDate(token, dependencyList);;
			break;
			case "NUMBER" : processNumber(token,dependencyList);
			break;
			case "DURATION" : ;
			case "TIME" : ;
			}
		}
		generateSumoDateTerms();
		return sumoTerms;
	}
}
