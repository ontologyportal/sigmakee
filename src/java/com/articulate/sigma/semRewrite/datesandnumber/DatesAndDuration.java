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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.articulate.sigma.semRewrite.datesandnumber.DateAndNumbersGeneration.DateComponent;

import edu.stanford.nlp.ling.IndexedWord;

public class DatesAndDuration {
	
	static final Pattern DIGITAL_YEAR_PATTERN = Pattern.compile("^[0-9]{4}$");
	static final Pattern WESTERN_YEAR_PATTERN = Pattern.compile("^([0-9]{1,2})(\\/|\\-|\\.)([0-9]{1,2})(\\/|\\-|\\.)([0-9]{4})$");
	static final Pattern DAY_PATTERN = Pattern.compile("^[0-9]{1,2}$");
	
	/** ***************************************************************
	 */
	 public void processDate(Tokens token, List<String> dependencyList, Utilities utilities) {

		Matcher digitalPatternMatcher = DIGITAL_YEAR_PATTERN.matcher(token.getWord());
		Matcher westernYearMatcher = WESTERN_YEAR_PATTERN.matcher(token.getWord());
		Matcher dayMatcher = DAY_PATTERN.matcher(token.getWord());
		if (Utilities.MONTHS.contains(token.getWord().toLowerCase())) {
			utilities.dateMap.put(token.getId(), "MONTH@"+token.getWord());
		} 
		else if (Utilities.DAYS.contains(token.getWord().toLowerCase())) {
			String tokenRoot = utilities.getRootWord(token.getId());
			if (tokenRoot != null) {
				utilities.sumoTerms.add("time("+tokenRoot+","+"time-"+utilities.timeCount+")");
			}
			DateInfo tempDate = new DateInfo();
			tempDate.setWeekDay(token.getWord());
			tempDate.addWordIndex(token.getId());
			tempDate.setTimeCount(utilities.timeCount);
			utilities.allDatesList.add(tempDate);
			utilities.sumoTerms.add("day(time-"+utilities.timeCount+","+token.getWord()+"-"+token.getId()+")");	
			utilities.timeCount++;
		} 
		else if (digitalPatternMatcher.find()) {
			utilities.dateMap.put(token.getId(), "YEAR@" + token.getWord());
		} 
		else if (westernYearMatcher.find()) {
			String tokenRoot = utilities.getRootWord(token.getId());
			if (tokenRoot != null) {
				utilities.sumoTerms.add("time("+tokenRoot+","+"time-"+utilities.timeCount+")");
			}
			DateInfo tempDate = new DateInfo();
			tempDate.setDay(westernYearMatcher.group(3));
			tempDate.setMonth(Utilities.MONTHS.get(Integer.valueOf(westernYearMatcher.group(1))-1));
			tempDate.setYear(westernYearMatcher.group(5));
			tempDate.addWordIndex(token.getId());
			tempDate.setTimeCount(utilities.timeCount);
			utilities.allDatesList.add(tempDate);
			utilities.sumoTerms.add("month(time-"+utilities.timeCount+","+ Utilities.MONTHS.get(Integer.valueOf(westernYearMatcher.group(1))-1)+"-"+token.getId()+")");
			utilities.sumoTerms.add("day(time-"+utilities.timeCount+","+westernYearMatcher.group(3)+"-"+token.getId()+")");
			utilities.sumoTerms.add("year(time-"+utilities.timeCount+","+westernYearMatcher.group(5)+"-"+token.getId()+")");
			utilities.timeCount++;
		} 
		else if (dayMatcher.find()) {
			utilities.dateMap.put(token.getId(), "DAYS@" + token.getWord());
		}
	}
	
	 /** ***************************************************************
		 */
	 public List<DateInfo> generateSumoDateTerms(Utilities utilities){

		//List<DateInfo> dateList = gatherDateSet(utilities);
		List<DateInfo> dateList = mergeDateSet(utilities);
		for (DateInfo date : dateList) {
			if ((date.getYear() != null) || (date.getMonth() != null) || (date.getDay() != null)) {
				if (date.getDay() != null) {
					utilities.sumoTerms.add("day(time-"+utilities.timeCount+","+date.getDay()+"-"+date.getWordIndex()+")");
				}
				if (date.getMonth() != null) {
					utilities.sumoTerms.add("month(time-"+utilities.timeCount+","+date.getMonth()+"-"+date.getWordIndex()+")");
				}
				if (date.getYear() != null) {
					utilities.sumoTerms.add("year(time-"+utilities.timeCount+","+date.getYear()+"-"+date.getWordIndex()+")");
				}
				String tokenRoot = utilities.getRootWord(date.getWordIndex());
				date.setTimeCount(utilities.timeCount);
				if (tokenRoot != null) {				
					utilities.sumoTerms.add("time("+tokenRoot+","+"time-"+utilities.timeCount+")");
				}
				utilities.timeCount++;
			}
		}
		return dateList;
	}
	
	 /** ***************************************************************
		 */
	 public List<DateInfo> gatherDateSet(Utilities utilities) {

		FlagUtilities flags = new FlagUtilities();
		DateInfo dateInfo = new DateInfo();
		List<DateInfo> dateList = new ArrayList<DateInfo>();
		DateInfo dateInfoTemp;
		String wordToken;
		Iterator<HashMap.Entry<Integer, String>> dateEntries = utilities.dateMap.entrySet().iterator();
		while (dateEntries.hasNext()) {
			HashMap.Entry<Integer, String> dateEntry = dateEntries.next();
			wordToken = dateEntry.getValue().split("@")[1];

			if (dateEntry.getValue().contains("MONTH")) {
				if (!flags.isMonthFlag()){
					populateDate(flags, DateComponent.MONTH, wordToken, dateInfo,dateEntry.getKey());
				}
				else if (!flags.isDayFlag() && !flags.isYearFlag()) {
					dateList.add(dateInfo);
					utilities.allDatesList.add(dateInfo);
					dateInfoTemp = new DateInfo();
					dateInfoTemp.setMonth(wordToken);
					dateInfoTemp.addWordIndex(dateEntry.getKey());
					dateList.add(dateInfoTemp);
					utilities.allDatesList.add(dateInfoTemp);
					flags.setMonthFlag(true);

				}
				else {
					dateInfo = addAndResetFlags(dateInfo, dateList, flags, DateComponent.MONTH, wordToken,dateEntry.getKey(), utilities);
				}
			}
			else if (dateEntry.getValue().contains("DAYS")) {
				if (!flags.isDayFlag()){
					populateDate(flags, DateComponent.DAY, wordToken, dateInfo,dateEntry.getKey());
				}
				else if (!flags.isMonthFlag() && !flags.isYearFlag()) {
					dateList.add(dateInfo);
					utilities.allDatesList.add(dateInfo);
					dateInfoTemp = new DateInfo();
					dateInfoTemp.setDay(wordToken);
					dateInfoTemp.addWordIndex(dateEntry.getKey());
					dateList.add(dateInfoTemp);
					utilities.allDatesList.add(dateInfoTemp);
					flags.setDayFlag(true);
				}
				else {
					dateInfo = addAndResetFlags(dateInfo, dateList, flags, DateComponent.DAY, wordToken,dateEntry.getKey(), utilities);
				}
			}
			else if (dateEntry.getValue().contains("YEAR")){
				if (!flags.isYearFlag()) {
					populateDate(flags, DateComponent.YEAR, wordToken, dateInfo,dateEntry.getKey());
				}
				else if (!flags.isDayFlag() && !flags.isMonthFlag()){
					dateList.add(new DateInfo(dateInfo));
					utilities.allDatesList.add(dateInfo);
					utilities.allDatesList.add(new DateInfo(dateInfo));
					dateInfo.clear();
					dateInfoTemp = new DateInfo();
					dateInfoTemp.setYear(wordToken);
					dateInfoTemp.addWordIndex(dateEntry.getKey());
					dateList.add(dateInfoTemp);
					utilities.allDatesList.add(dateInfoTemp);
					flags.setYearFlag(true);
				}
				else {
					dateInfo = addAndResetFlags(dateInfo, dateList, flags, DateComponent.YEAR, wordToken,dateEntry.getKey(), utilities);
				}	
			}
			else if (flags.isYearFlag() && flags.isMonthFlag() && flags.isDayFlag()) {
				dateInfoTemp = new DateInfo(dateInfo);
				dateList.add(dateInfoTemp);
				utilities.allDatesList.add(dateInfoTemp);
				dateInfoTemp.addWordIndex(dateEntry.getKey());
				dateInfo.clear();
				flags.resetFlags();
			}
		}
		if (!dateList.contains(dateInfo) && !dateInfo.isEmpty()) {
			dateList.add(dateInfo);
			utilities.allDatesList.add(dateInfo);
		}
		return dateList;
	}
	 
	 /** ***************************************************************
		 */
	 public List<DateInfo> mergeDateSet(Utilities utilities) {
	     
		 List<DateInfo> dateList = new ArrayList<DateInfo>();
		 Iterator<HashMap.Entry<Integer, String>> dateEntries = utilities.dateMap.entrySet().iterator();
		 List<String> monthDateGroup = new ArrayList<String>();
		 List<String> dateDateGroup = new ArrayList<String>();
		 List<String> yearGroup = new ArrayList<String>();
		 int count = 0;
		 int prevIndex = -1;
		 String wordToken;
		 	while (dateEntries.hasNext()){
		 		HashMap.Entry<Integer, String> dateEntry = dateEntries.next();
		 		wordToken = dateEntry.getValue().split("@")[1];
		 		if (dateEntry.getValue().contains("MONTH") ) {
		 			if (count == 0) {
		 				monthDateGroup.add(dateEntry.getKey() + "::" + dateEntry.getValue());
			 			count++;
			 			prevIndex = dateEntry.getKey();
		 			}
		 			else if(count == 1) {
		 				if (dateDateGroup.size() == 1 && (dateEntry.getKey() - prevIndex) <= 2) {
		 					dateDateGroup.add(dateEntry.getKey() + "::" + dateEntry.getValue());
		 					count ++;
		 					prevIndex = dateEntry.getKey();
		 				} 
		 				else if (dateEntry.getKey() - prevIndex >= 2) {
		 					if (monthDateGroup.size() != 0) {
			 					addDateInfoToList(monthDateGroup, dateList, utilities);
			 					monthDateGroup.clear();
					 			dateDateGroup.clear();
					 			yearGroup.clear();
					 			count = 0;
					 			monthDateGroup.add(dateEntry.getKey() + "::" + dateEntry.getValue());
					 			count++;
					 			prevIndex = dateEntry.getKey();
			 				} 
		 					else if (dateDateGroup.size() != 0) {
			 					addDateInfoToList(dateDateGroup, dateList, utilities);
			 					monthDateGroup.clear();
					 			dateDateGroup.clear();
					 			yearGroup.clear();
					 			count = 0;
					 			monthDateGroup.add(dateEntry.getKey() + "::" + dateEntry.getValue());
					 			count++;
					 			prevIndex = dateEntry.getKey();
			 				} 
		 				}
		 			} 
		 			else {
		 				if (dateEntry.getKey() - prevIndex >= 2) {
		 					if (monthDateGroup.size() != 0 ) {
			 					addDateInfoToList(monthDateGroup, dateList, utilities);
			 					monthDateGroup.clear();
					 			dateDateGroup.clear();
					 			yearGroup.clear();
					 			count = 0;
					 			monthDateGroup.add(dateEntry.getKey() + "::" + dateEntry.getValue());
					 			count++;
					 			prevIndex = dateEntry.getKey();
			 				} 
		 					else if (dateDateGroup.size() != 0) {
			 					addDateInfoToList(dateDateGroup, dateList, utilities);
			 					monthDateGroup.clear();
					 			dateDateGroup.clear();
					 			yearGroup.clear();
					 			count = 0;
					 			monthDateGroup.add(dateEntry.getKey() + "::" + dateEntry.getValue());
					 			count++;
					 			prevIndex = dateEntry.getKey();
			 				} 
		 				}
		 			}
		 			
		 		}
		 		else if (dateEntry.getValue().contains("DAY") ) {
		 			if (count == 0) {
		 				dateDateGroup.add(dateEntry.getKey() + "::" + dateEntry.getValue());
			 			count++;
			 			prevIndex = dateEntry.getKey();
		 			}
		 			else if (count == 1) {
		 				if (monthDateGroup.size() == 1 && (dateEntry.getKey()-prevIndex) <= 2) {
		 					monthDateGroup.add(dateEntry.getKey() + "::" + dateEntry.getValue());
		 					count ++;
		 					prevIndex = dateEntry.getKey();
		 				} 
		 				else if (dateEntry.getKey() - prevIndex >= 2) {
		 					if (monthDateGroup.size() != 0 ) {
			 					addDateInfoToList(monthDateGroup, dateList, utilities);
			 					monthDateGroup.clear();
					 			dateDateGroup.clear();
					 			yearGroup.clear();
					 			count = 0;
					 			dateDateGroup.add(dateEntry.getKey() + "::" + dateEntry.getValue());
					 			count++;
					 			prevIndex = dateEntry.getKey();
			 				} 
		 					else if (dateDateGroup.size() != 0 ) {
			 					addDateInfoToList(dateDateGroup, dateList, utilities);
			 					monthDateGroup.clear();
					 			dateDateGroup.clear();
					 			yearGroup.clear();
					 			count = 0;
					 			dateDateGroup.add(dateEntry.getKey() + "::" + dateEntry.getValue());
					 			count++;
					 			prevIndex = dateEntry.getKey();
			 				} 
		 				}
		 			}
		 			else {
		 				if (dateEntry.getKey() - prevIndex >= 2) {
		 					if (monthDateGroup.size() != 0 ) {
			 					addDateInfoToList(monthDateGroup, dateList, utilities);
			 					monthDateGroup.clear();
					 			dateDateGroup.clear();
					 			yearGroup.clear();
					 			count = 0;
					 			dateDateGroup.add(dateEntry.getKey() + "::" + dateEntry.getValue());
					 			count++;
					 			prevIndex = dateEntry.getKey();
			 				} 
		 					else if (dateDateGroup.size() != 0 ) {
			 					addDateInfoToList(dateDateGroup, dateList, utilities);
			 					monthDateGroup.clear();
					 			dateDateGroup.clear();
					 			yearGroup.clear();
					 			count = 0;
					 			dateDateGroup.add(dateEntry.getKey()+ "::" + dateEntry.getValue());
					 			count++;
					 			prevIndex = dateEntry.getKey();
			 				} 
		 				}
		 			}
		 			
		 		}
		 		else if (dateEntry.getValue().contains("YEAR")) { 
		 			if (count == 0) {
		 				DateInfo dateInfo = new DateInfo();
		 				dateInfo.setYear(wordToken);
		 				dateInfo.addWordIndex(dateEntry.getKey());
		 				dateList.add(dateInfo);
		 				utilities.allDatesList.add(dateInfo);
		 				prevIndex = dateEntry.getKey();
		 			} 
		 			else {
		 				if (monthDateGroup.size() != 0 && (dateEntry.getKey() - prevIndex) <= 2) {
		 					monthDateGroup.add(dateEntry.getKey()+"::"+dateEntry.getValue());
		 					addDateInfoToList(monthDateGroup, dateList, utilities);
		 					monthDateGroup.clear();
				 			dateDateGroup.clear();
				 			yearGroup.clear();
				 			count = 0;
		 				} 
		 				else if (dateDateGroup.size() != 0 && (dateEntry.getKey()-prevIndex) <= 2) {
		 					dateDateGroup.add(dateEntry.getKey() + "::" + dateEntry.getValue());
		 					addDateInfoToList(dateDateGroup, dateList, utilities);
		 					monthDateGroup.clear();
				 			dateDateGroup.clear();
				 			yearGroup.clear();
				 			count = 0;
		 				}
		 				
		 			}
		 		}
		 		else if (count == 3) {
		 			if (monthDateGroup.size() == 3) {
		 				addDateInfoToList(monthDateGroup, dateList, utilities);
		 			} 
		 			else if (dateDateGroup.size() == 3) {
		 				addDateInfoToList(dateDateGroup, dateList, utilities);
		 			}
		 			monthDateGroup.clear();
		 			dateDateGroup.clear();
		 			yearGroup.clear();
		 			count = 0;
		 		}
		 	}
		 return dateList;
	 }
	 
	 /** ***************************************************************
		 */
	 public void addDateInfoToList(List<String> dateGroup, List<DateInfo> dateList, Utilities utilities) {
	     
		 DateInfo dateInfo = new DateInfo();
			for (String data : dateGroup) {
				int index = Integer.valueOf(data.split("::")[0]);
				String word = data.split("::")[1].split("@")[1];
				String type = data.split("::")[1].split("@")[0];
				switch(type) {
					case "MONTH": dateInfo.setMonth(word);
								break;
					case "YEAR": dateInfo.setYear(word);
								 break;
					case "DAYS": dateInfo.setDay(word);
					             break;
				}
				dateInfo.addWordIndex(index);
			}
			
			dateList.add(dateInfo);
			utilities.allDatesList.add(dateInfo);
	 }
	 
	 /** ***************************************************************
		 */
	 public DateInfo addAndResetFlags(DateInfo dateSet, List<DateInfo> dateList, FlagUtilities flags, 
			 DateComponent dateComponent, String token,int wordId, Utilities utilities) {

		DateInfo dateSetTemp;
		dateSetTemp = new DateInfo(dateSet);
		dateList.add(dateSetTemp);
		utilities.allDatesList.add(dateSetTemp);
		dateSet.clear();
		flags.resetFlags();
		DateInfo newDateInfo = new DateInfo();
		switch (dateComponent) {
			case DAY :
				newDateInfo.setDay(token);
				newDateInfo.addWordIndex(wordId);
				flags.setDayFlag(true);
				break;
			case MONTH:
				newDateInfo.setMonth(token);
				newDateInfo.addWordIndex(wordId);
				flags.setMonthFlag(true);
				break;
			case YEAR:
				newDateInfo.setYear(token);
				newDateInfo.addWordIndex(wordId);
				flags.setYearFlag(true);
		}
		return newDateInfo;
	}
	
	 /** ***************************************************************
		 */
	 public void populateDate(FlagUtilities flags,DateComponent dateComponent, String wordToken, DateInfo dateSet,int wordId) {

		switch(dateComponent) {
		case MONTH : flags.setMonthFlag(true);
					 dateSet.setMonth(wordToken);
					 dateSet.addWordIndex(wordId);
					 break;
		case YEAR : flags.setYearFlag(true);
					dateSet.setYear(wordToken);
					dateSet.addWordIndex(wordId);
					break;
		case DAY : flags.setDayFlag(true);
				   dateSet.setDay(wordToken);
				   dateSet.addWordIndex(wordId);
				   break;
		}
	}
	 
	 /** ***************************************************************
		 */
	 public void processDuration(Tokens token, Utilities utilities) {
		
		// System.out.println("Duration word is ::" + token.getWord());
		 if (token.getWord().matches("[0-9]{4}\\-[0-9]{4}")) {
			 String years[] = token.getWord().split("-");
			 IndexedWord tempParent = utilities.StanfordDependencies.getNodeByIndex(token.getId());
			 tempParent = getAssociatedWord(utilities, tempParent);
			 DateInfo newDateInfo = new DateInfo();
			 newDateInfo.setYear(years[0]);
			 newDateInfo.addWordIndex(token.getId());
			 newDateInfo.setTimeCount(utilities.timeCount);
			 utilities.sumoTerms.add("year(time-"+utilities.timeCount+","+years[0]+"-"+token.getId()+")");
			 utilities.timeCount++;
			 
			 DateInfo endDateInfo = new DateInfo();
			 endDateInfo.setYear(years[1]);
			 endDateInfo.addWordIndex(token.getId());
			 endDateInfo.setTimeCount(utilities.timeCount);
			 utilities.sumoTerms.add("year(time-"+utilities.timeCount+","+years[1]+"-"+token.getId()+")");
			 utilities.timeCount++;
			 
			 generateDurationSumoTerms(tempParent,utilities, newDateInfo, endDateInfo);
		 }
	 }
	 
	 /** ***************************************************************
		 */
	 public void generateDurationSumoTerms(IndexedWord tempParent, Utilities utilities, DateInfo startDateInfo, DateInfo endDateInfo) {
	     
	     if (tempParent != null) {
	         if (Utilities.VerbTags.contains(tempParent.tag())) {
	             utilities.sumoTerms.add("StartTime(" + tempParent.value()+"-"+tempParent.index() + "," + "time-" + startDateInfo.getTimeCount() + ")");
	             utilities.sumoTerms.add("EndTime(" + tempParent.value() +"-"+tempParent.index()+ "," + "time-" + endDateInfo.getTimeCount() + ")");

	         }
	         if (Utilities.nounTags.contains(tempParent.tag())) {
	             if (tempParent.ner().equals("PERSON")) {
	                 utilities.sumoTerms.add("BirthDate(" + tempParent.value() +"-"+tempParent.index()+ "," + "time-" + startDateInfo.getTimeCount() + ")");
	                 utilities.sumoTerms.add("DeathDate(" + tempParent.value() +"-"+tempParent.index()+ "," + "time-" + endDateInfo.getTimeCount() + ")");
	             } 
	             else {
	                 utilities.sumoTerms.add("StartTime(" + tempParent.value() +"-"+tempParent.index()+ "," + "time-" + startDateInfo.getTimeCount() + ")");
	                 utilities.sumoTerms.add("EndTime(" + tempParent.value() +"-"+tempParent.index()+ "," + "time-" + endDateInfo.getTimeCount() + ")");
	             }
	         }
	         startDateInfo.setDurationFlag(true);
	         endDateInfo.setDurationFlag(true);
	     }
	 }
	
	 /** ***************************************************************
		 */
	 public IndexedWord getAssociatedWord(Utilities utilities, IndexedWord tempParent) {

	     while (!tempParent.equals(utilities.StanfordDependencies.getFirstRoot())) {
	         tempParent = utilities.StanfordDependencies.getParent(tempParent);
	         if (Utilities.VerbTags.contains(tempParent.tag()) ||
	                 Utilities.nounTags.contains(tempParent.tag())) {
	             break;
	         }
	     }
	     return tempParent;
	 }
	 
	 /** ***************************************************************
		 */
	 public void handleDurations(Utilities utilities) {

		for (int i = 0; i < utilities.allDatesList.size() - 1; i++) {
			if ((utilities.allDatesList.get(i).getEndIndex() + 2) == (utilities.allDatesList.get(i + 1).getWordIndex())) {
				utilities.allDatesList.get(i).setDurationFlag(true);
				utilities.allDatesList.get(i+1).setDurationFlag(true);
				//utilities.allDatesList.get(i).print();
				//utilities.allDatesList.get(i+1).print();
				IndexedWord tempParent = utilities.StanfordDependencies.getNodeByIndex(utilities.allDatesList.get(i).getWordIndex());	
				tempParent = getAssociatedWord(utilities, tempParent);
				generateDurationSumoTerms(tempParent, utilities, utilities.allDatesList.get(i), utilities.allDatesList.get(i+1));
			}
		}
	}
}
