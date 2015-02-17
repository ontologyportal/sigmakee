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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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
	static final Pattern HOUR_MINUTE_PATTERN = Pattern.compile("^T([0-9]{2}):([0-9]{2})$");
	static final Pattern HOUR_MINUTE_SECOND_PATTERN = Pattern.compile("^T([0-9]{2}):([0-9]{2}):([0-9]{2})$");
	static final Pattern YEAR_MONTH_TIME_PATTERN = Pattern.compile("^([0-9X]{4})(\\-[0-9]{2})?(\\-[0-9]{2})?T([0-9]{2}):([0-9]{2})(:[0-9]{2})?");

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
		Iterator<HashMap.Entry<Integer, String>> dateEntries = dateMap.entrySet().iterator();
		while (dateEntries.hasNext()){
			
			HashMap.Entry<Integer, String> dateEntry = dateEntries.next();
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
	private void generateSumoTimeTerms(List<TimeInfo> timesList) {
		
		for (TimeInfo times : timesList) {
			StringBuffer timeFn = new StringBuffer();
			if (times.getSecond() != null) {
				timeFn.append(" (" + "SecondFn" + " " + times.getSecond());
			}
			if (times.getMinute() != null) {
				timeFn.append(" (" + "MinuteFn" + " " + times.getMinute());
			}
			if (times.getHour() != null) {
				timeFn.append(" (" + "HourFn" + " " + times.getHour());
			}
			if (timeFn.length() == 0) {
				return;
			}
			int charCount = timeFn.toString().replaceAll("[^(]", "").length();
			for (int i = 0; i <charCount; ++i) {
				timeFn.append(")");
			}
			sumoTerms.add("(during" + rootWord + timeFn.toString());
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
	private List<String> getUniqueTimeTokens(List<String> tokenIdNormalizedTimeMap) {
		
		int presentTokenId;
		int nextTokenId;
		String presentTimeToken;
		String nextTimeToken;
		HashSet<String> consecutiveTimes = new HashSet<String>();
		List<String> uniqueTimeTokens = new ArrayList<String>();
		for (int i = 0; i < tokenIdNormalizedTimeMap.size() - 1 ; i++) {
			presentTokenId = Integer.valueOf(tokenIdNormalizedTimeMap.get(i).split("@")[0]);
			nextTokenId = Integer.valueOf(tokenIdNormalizedTimeMap.get(i + 1).split("@")[0]);
			presentTimeToken = tokenIdNormalizedTimeMap.get(i).split("@")[1];
			nextTimeToken = tokenIdNormalizedTimeMap.get(i + 1).split("@")[1];
			if ((presentTokenId + 1 != nextTokenId) && presentTimeToken.equals(nextTimeToken)) {
				uniqueTimeTokens.add(tokenIdNormalizedTimeMap.get(i));
			} else if ((presentTokenId + 1 == nextTokenId) && !presentTimeToken.equals(nextTimeToken)) {
				uniqueTimeTokens.add(tokenIdNormalizedTimeMap.get(i));
				uniqueTimeTokens.add(tokenIdNormalizedTimeMap.get(i+1));
			} else if ((presentTokenId + 1 != nextTokenId) && !presentTimeToken.equals(nextTimeToken)) {
				uniqueTimeTokens.add(tokenIdNormalizedTimeMap.get(i));
				uniqueTimeTokens.add(tokenIdNormalizedTimeMap.get(i+1));
			} else if ((presentTokenId + 1 == nextTokenId) && presentTimeToken.equals(nextTimeToken)) {
				if (!consecutiveTimes.contains(presentTimeToken)) {
					consecutiveTimes.add(presentTimeToken);
					uniqueTimeTokens.add(tokenIdNormalizedTimeMap.get(i));
				}
				i = i + 1;
			}
		}
		return uniqueTimeTokens;
	}
	
	/** ***************************************************************
	 */
	private List<TimeInfo> processTime(List<String> tokenIdNormalizedTimeMap) {
		
		List<TimeInfo> timesList = new ArrayList<TimeInfo>();
		List<String> uniqueTimeTokens = getUniqueTimeTokens(tokenIdNormalizedTimeMap);
		for (String timeToken : uniqueTimeTokens) {
			int id = Integer.valueOf(timeToken.split("@")[0]);
			String timeStr = timeToken.split("@")[1];
			Matcher hourMinPatternMatcher = HOUR_MINUTE_PATTERN.matcher(timeStr);
			Matcher hourMinSecPatternMatcher = HOUR_MINUTE_SECOND_PATTERN.matcher(timeStr);
			Matcher yearMonthTimePatternMatcher = YEAR_MONTH_TIME_PATTERN.matcher(timeStr);
			TimeInfo timeObj = new TimeInfo();
			if (hourMinPatternMatcher.find()) {
				timeObj.setMinute(hourMinPatternMatcher.group(2));
				timeObj.setHour(hourMinPatternMatcher.group(1));
			} else if (hourMinSecPatternMatcher.find()) {
				timeObj.setMinute(hourMinSecPatternMatcher.group(2));
				timeObj.setHour(hourMinSecPatternMatcher.group(1));
				timeObj.setSecond(hourMinSecPatternMatcher.group(3));
			} else if (yearMonthTimePatternMatcher.find()) {
				String year = yearMonthTimePatternMatcher.group(1);
				int tokenCnt = new StanfordDateTimeExtractor().getTokenCount() + 1;
				if (!year.equals("XXXX")) {
					if(!checkValueInMap("YEAR@" + year)) {
						dateMap.put(id, "YEAR@" + year);
					}
				}
				if (yearMonthTimePatternMatcher.group(2) != null) {
					String month = yearMonthTimePatternMatcher.group(2).replaceAll("\\-", "");
					if(!checkValueInMap("MONTH@" + month) && !checkValueInMap("MONTH@" + MONTHS.get(Integer.valueOf(month) - 1))) {
						dateMap.put(tokenCnt + id, "MONTH@" + MONTHS.get(Integer.valueOf(month) - 1));
						tokenCnt ++;
					}
				}
				if (yearMonthTimePatternMatcher.group(3) != null) {
					String day = yearMonthTimePatternMatcher.group(3).replaceAll("\\-", "");
					if(!checkValueInMap("DAYS@" + day)) {
						dateMap.put(tokenCnt + id, "DAYS@" + day);
					}
				}
				timeObj.setMinute(yearMonthTimePatternMatcher.group(5));
				timeObj.setHour(yearMonthTimePatternMatcher.group(4));
				if (yearMonthTimePatternMatcher.group(6) != null) {
					timeObj.setSecond(yearMonthTimePatternMatcher.group(6));
				}
				
			}
			timesList.add(timeObj);
		}
		return timesList;
	}
	
	private boolean checkValueInMap(String value) {
		for (int key : dateMap.keySet()) {
			if (dateMap.get(key).equalsIgnoreCase(value)) {
				return true;
			}
		}
		return false;
	}

	/** ***************************************************************
	 */
	public List<String> generateSumoTerms(List<Tokens> tokensList, List<String> dependencyList) {
		
		setRootWord(dependencyList);
		List<String> tokenIdNormalizedTimeMap = new ArrayList<String>();
		for(Tokens token : tokensList) {
			switch(token.getNer()) {
			case "DATE"  : processDate(token, dependencyList);
			break;
			case "NUMBER" : processNumber(token,dependencyList);
			break;
			case "DURATION" : ;
			case "TIME" : tokenIdNormalizedTimeMap.add(token.getId() + "@" + token.getNormalizedNer());
			}
		}
		List<TimeInfo> timesList = processTime(tokenIdNormalizedTimeMap);
		generateSumoDateTerms();
		generateSumoTimeTerms(timesList);
		return sumoTerms;
	}
}
