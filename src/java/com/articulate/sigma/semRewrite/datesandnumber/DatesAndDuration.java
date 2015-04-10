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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.stanford.nlp.ling.IndexedWord;

public class DatesAndDuration {
	
	static final Pattern DIGITAL_YEAR_PATTERN = Pattern.compile("^[0-9]{4}$");
	static final Pattern WESTERN_YEAR_PATTERN = Pattern.compile("^([0-9]{1,2})(\\/|\\-|\\.)([0-9]{1,2})(\\/|\\-|\\.)([0-9]{4})$");
	static final Pattern DAY_PATTERN = Pattern.compile("^[0-9]{1,2}(th)?$");
	static final Pattern DURATION_PATTERN = Pattern.compile("^P([0-9])+[A-Z]");
	
	/** ***************************************************************
	 */
	 public void processDate(Tokens token, Utilities utilities, List<Tokens> tempDateList, Tokens prevToken) {

		Matcher digitalPatternMatcher = DIGITAL_YEAR_PATTERN.matcher(token.getWord());
		Matcher westernYearMatcher = WESTERN_YEAR_PATTERN.matcher(token.getWord());
		Matcher dayMatcher = DAY_PATTERN.matcher(token.getWord());
		if (Utilities.MONTHS.contains(token.getWord().toLowerCase())) {
			token.setTokenType("MONTH");
			mergeDates(token, tempDateList, prevToken, utilities);
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
			utilities.datesList.add(tempDate);
			utilities.sumoTerms.add("day(time-"+utilities.timeCount+","+token.getWord()+")");	
			utilities.timeCount++;
		} 
		else if (digitalPatternMatcher.find()) {
			token.setTokenType("YEAR");
			mergeDates(token, tempDateList, prevToken, utilities);
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
			utilities.datesList.add(tempDate);
		} 
		else if (dayMatcher.find()) {
			if(token.getWord().contains("th")) {
				token.setWord(token.getWord().replace("th", ""));
			}
			token.setTokenType("DAYS");
			mergeDates(token, tempDateList, prevToken, utilities);
		}
	}
	 
	 /** ***************************************************************
		 */
	 private void addInfoToDateList(List<Tokens> tempDateList, Utilities utilities) {
		 DateInfo tempDateInfo = new DateInfo();
		 for(Tokens token : tempDateList) {
			 switch(token.getTokenType()) {
			 case "MONTH" : tempDateInfo.setMonth(token.getWord());
				 			break;
			 case "YEAR" : tempDateInfo.setYear(token.getWord());
				           break;
			 case "DAYS" : tempDateInfo.setDay(token.getWord());
				            break;
			 }
			 tempDateInfo.addWordIndex(token.getId());
		 }
		 if(!tempDateInfo.isEmpty()) {
			 utilities.datesList.add(tempDateInfo);
		 }
		 
	 }
	 
	 /** ***************************************************************
		 */
	 private boolean checkTokenInList(List<Tokens> tempDateList, Tokens token) {
		 for(Tokens tempToken : tempDateList) {
			 if(tempToken.getTokenType().equals(token.getTokenType())) {
				 return false;
			 }
		 }
		 return true;
	 }
	 
	 /** ***************************************************************
		 */
	 private void clearTempDateList(List<Tokens> tempDateList, Utilities utilities, Tokens token) {
		 addInfoToDateList(tempDateList, utilities);
		 tempDateList.clear();
		 tempDateList.add(token);
	 }
	 
	 /** ***************************************************************
		 */
	 public void mergeDates(Tokens token, List<Tokens> tempDateList, Tokens prevToken, Utilities utilities) {
		 
		 if(prevToken == null) {
			 tempDateList.add(token);
			 return;
		 }
		 if((token.getId() - prevToken.getId()) <= 2 && !token.getTokenType().equals(prevToken.getTokenType())) {
			 if(checkTokenInList(tempDateList,token)) {
				 tempDateList.add(token);
			 } else {
				 clearTempDateList(tempDateList, utilities, token);
			 }
			 
		 } else {
			 clearTempDateList(tempDateList, utilities, token);
		 }
	 }
	 
	 /** ***************************************************************
		 */
	 public List<DateInfo> generateSumoDateTerms(Utilities utilities, List<Tokens> tempDateList){

		 if(!tempDateList.isEmpty()) {
			 addInfoToDateList(tempDateList, utilities);
			 tempDateList.clear();
		 }
		List<DateInfo> dateList = utilities.datesList;
		for (DateInfo date : dateList) {
			if ((date.getYear() != null) || (date.getMonth() != null) || (date.getDay() != null)) {
				if (date.getDay() != null) {
					utilities.sumoTerms.add("day(time-"+utilities.timeCount+","+date.getDay()+")");
				}
				if (date.getMonth() != null) {
					utilities.sumoTerms.add("month(time-"+utilities.timeCount+","+date.getMonth()+")");
				}
				if (date.getYear() != null) {
					utilities.sumoTerms.add("year(time-"+utilities.timeCount+","+date.getYear()+")");
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
	 public Tokens processDuration(Tokens token, Utilities utilities, Tokens prevDurationToken) {
		
		 if (token.getWord().matches("[0-9]{4}\\-[0-9]{4}")) {
			 String years[] = token.getWord().split("-");
			 IndexedWord tempParent = utilities.StanfordDependencies.getNodeByIndex(token.getId());
			 tempParent = getAssociatedWord(utilities, tempParent);
			 DateInfo newDateInfo = new DateInfo();
			 newDateInfo.setYear(years[0]);
			 newDateInfo.addWordIndex(token.getId());
			 newDateInfo.setTimeCount(utilities.timeCount);
			 utilities.sumoTerms.add("year(time-"+utilities.timeCount+","+years[0]+")");
			 utilities.timeCount++;
			 
			 DateInfo endDateInfo = new DateInfo();
			 endDateInfo.setYear(years[1]);
			 endDateInfo.addWordIndex(token.getId());
			 endDateInfo.setTimeCount(utilities.timeCount);
			 utilities.sumoTerms.add("year(time-"+utilities.timeCount+","+years[1]+")");
			 utilities.timeCount++;
			 
			 generateDurationSumoTerms(tempParent,utilities, newDateInfo, endDateInfo);
		 } else  {
			 if(prevDurationToken == null) {
				 Matcher durationMatcher = DURATION_PATTERN.matcher(token.getNormalizedNer());
				 if(durationMatcher.find()) {
					 return token;
				 }
			 } else if(!(token.getNormalizedNer().equals(prevDurationToken.getNormalizedNer())) || !(token.getId() - 1 == prevDurationToken.getId())) {
				 Matcher durationMatcher = DURATION_PATTERN.matcher(token.getNormalizedNer());
				 if(durationMatcher.find()) {
					 return token;
				 }
			 }
		 }
		 return null;
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

		for (int i = 0; i < utilities.datesList.size() - 1; i++) {
			if ((utilities.datesList.get(i).getEndIndex() + 2) == (utilities.datesList.get(i + 1).getWordIndex())) {
				utilities.datesList.get(i).setDurationFlag(true);
				utilities.datesList.get(i+1).setDurationFlag(true);
				IndexedWord tempParent = utilities.StanfordDependencies.getNodeByIndex(utilities.datesList.get(i).getWordIndex());	
				tempParent = getAssociatedWord(utilities, tempParent);
				generateDurationSumoTerms(tempParent, utilities, utilities.datesList.get(i), utilities.datesList.get(i+1));
			}
		}
	}
}
