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
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.articulate.sigma.KBmanager;
import com.articulate.sigma.WSD;
import com.articulate.sigma.WordNet;

import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.trees.GrammaticalRelation;

public class DateAndNumbersGeneration {

	static enum DateComponent {
		DAY, MONTH, YEAR
	}

	public final List<String> MONTHS = new ArrayList<String>(Arrays.asList("january",
			"february","march","april","may","june","july","august",
			"september","october","november","december"));
	public final List<String> DAYS = new ArrayList<String>(Arrays.asList("monday",
			"tuesday","wednesday","thursday","friday","saturday","sunday"));
	public final List<String> VerbTags = new ArrayList<String>(Arrays.asList("VB",
			"VBD","VBG","VBN","VBP","VBZ"));
	public final List<String> nounTags = new ArrayList<String>(Arrays.asList("NN","NNS","NNP","NNPS","/NN","/NNS","/NNP", "/NNPS"));


	static final Pattern DIGITAL_YEAR_PATTERN = Pattern.compile("^[0-9]{4}$");
	static final Pattern WESTERN_YEAR_PATTERN = Pattern.compile("^([0-9]{1,2})(\\/|\\-|\\.)([0-9]{1,2})(\\/|\\-|\\.)([0-9]{4})$");
	static final Pattern DAY_PATTERN = Pattern.compile("^[0-9]{1,2}$");
	static final Pattern HOUR_MINUTE_PATTERN = Pattern.compile("^T([0-9]{2}):([0-9]{2})$");
	static final Pattern HOUR_MINUTE_SECOND_PATTERN = Pattern.compile("^T([0-9]{2}):([0-9]{2}):([0-9]{2})$");
	static final Pattern YEAR_MONTH_TIME_PATTERN = Pattern.compile("^([0-9X]{4})(\\-[0-9]{2})?(\\-[0-9]{2})?T([0-9]{2}):([0-9]{2})(:[0-9]{2})?");
	static final Pattern POS_TAG_REMOVER = Pattern.compile("(\\/[A-Z]+)$");

	private List<String> sumoTerms = new ArrayList<String>();
	List<String> measureTerms = new ArrayList<String>();
	private HashMap<Integer,String> dateMap = new HashMap<Integer,String>();
	private SemanticGraph StanfordDependencies;
	private int timeCount = 1;
	private List<DateInfo> allDatesList = new LinkedList<DateInfo>();
	
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
			String tokenRoot = getRootWord(token.getId());
			if (tokenRoot != null) {
				sumoTerms.add("time("+tokenRoot+","+"time-"+timeCount+")");
			}
			DateInfo tempDate = new DateInfo();
			tempDate.setWeekDay(token.getWord());
			tempDate.addWordIndex(token.getId());
			tempDate.setTimeCount(timeCount);
			allDatesList.add(tempDate);
			sumoTerms.add("day(time-"+timeCount+","+token.getWord()+")");	
			timeCount++;
		} 
		else if (digitalPatternMatcher.find()) {
			dateMap.put(token.getId(), "YEAR@" + token.getWord());
		} 
		else if (westernYearMatcher.find()) {
			String tokenRoot = getRootWord(token.getId());
			if (tokenRoot != null) {
				sumoTerms.add("time("+tokenRoot+","+"time-"+timeCount+")");
			}
			DateInfo tempDate = new DateInfo();
			tempDate.setDay(westernYearMatcher.group(3));
			tempDate.setMonth(MONTHS.get(Integer.valueOf(westernYearMatcher.group(1))-1));
			tempDate.setYear(westernYearMatcher.group(5));
			tempDate.addWordIndex(token.getId());
			tempDate.setTimeCount(timeCount);
			allDatesList.add(tempDate);
			sumoTerms.add("month(time-"+timeCount+","+MONTHS.get(Integer.valueOf(westernYearMatcher.group(1))-1)+")");
			sumoTerms.add("day(time-"+timeCount+","+westernYearMatcher.group(3)+")");
			sumoTerms.add("year(time-"+timeCount+","+westernYearMatcher.group(5)+")");
			timeCount++;
		} 
		else if (dayMatcher.find()) {
			dateMap.put(token.getId(), "DAYS@" + token.getWord());
		}
	}

	/** ***************************************************************
	 */
	private void populateDate(FlagUtilities flags,DateComponent dateComponent, String wordToken, DateInfo dateSet,int wordId) {

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
					populateDate(flags, DateComponent.MONTH, wordToken, dateInfo,dateEntry.getKey());
				}
				else if (!flags.isDayFlag() && !flags.isYearFlag()) {
					dateList.add(dateInfo);
					allDatesList.add(dateInfo);
					dateInfoTemp = new DateInfo();
					dateInfoTemp.setMonth(wordToken);
					dateInfoTemp.addWordIndex(dateEntry.getKey());
					dateList.add(dateInfoTemp);
					allDatesList.add(dateInfoTemp);
					flags.setMonthFlag(true);

				}
				else {
					dateInfo = addAndResetFlags(dateInfo, dateList, flags, DateComponent.MONTH, wordToken,dateEntry.getKey());
				}
			}
			else if (dateEntry.getValue().contains("DAYS")) {
				if (!flags.isDayFlag()){
					populateDate(flags, DateComponent.DAY, wordToken, dateInfo,dateEntry.getKey());
				}
				else if (!flags.isMonthFlag() && !flags.isYearFlag()) {
					dateList.add(dateInfo);
					allDatesList.add(dateInfo);
					dateInfoTemp = new DateInfo();
					dateInfoTemp.setDay(wordToken);
					dateInfoTemp.addWordIndex(dateEntry.getKey());
					dateList.add(dateInfoTemp);
					allDatesList.add(dateInfoTemp);
					flags.setDayFlag(true);
				}
				else {
					dateInfo = addAndResetFlags(dateInfo, dateList, flags, DateComponent.DAY, wordToken,dateEntry.getKey());
				}
			}
			else if (dateEntry.getValue().contains("YEAR")){
				if (!flags.isYearFlag()) {
					populateDate(flags, DateComponent.YEAR, wordToken, dateInfo,dateEntry.getKey());
				}
				else if (!flags.isDayFlag() && !flags.isMonthFlag()){
					dateList.add(dateInfo);
					allDatesList.add(dateInfo);
					dateInfoTemp = new DateInfo();
					dateInfoTemp.setYear(wordToken);
					dateInfoTemp.addWordIndex(dateEntry.getKey());
					dateList.add(dateInfoTemp);
					allDatesList.add(dateInfoTemp);
					flags.setYearFlag(true);
				}
				else {
					dateInfo = addAndResetFlags(dateInfo, dateList, flags, DateComponent.YEAR, wordToken,dateEntry.getKey());
				}	
			}
			else if (flags.isYearFlag() && flags.isMonthFlag() && flags.isDayFlag()) {
				dateInfoTemp = new DateInfo(dateInfo);
				dateList.add(dateInfoTemp);
				allDatesList.add(dateInfoTemp);
				dateInfoTemp.addWordIndex(dateEntry.getKey());
				dateInfo.clear();
				flags.resetFlags();
			}
		}
		if(!dateList.contains(dateInfo)) {
			dateList.add(dateInfo);
			allDatesList.add(dateInfo);
		}
		return dateList;
	}

	/** ***************************************************************
	 */
	private DateInfo addAndResetFlags(DateInfo dateSet, List<DateInfo> dateList, FlagUtilities flags, DateComponent dateComponent, String token,int wordId) {

		DateInfo dateSetTemp;
		dateSetTemp = new DateInfo(dateSet);
		dateList.add(dateSetTemp);
		allDatesList.add(dateSetTemp);
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
	private List<DateInfo> generateSumoDateTerms(){

		List<DateInfo> dateList = gatherDateSet();
		for (DateInfo date : dateList) {
			if ((date.getYear() != null) || (date.getMonth() != null) || (date.getDay() != null)) {
				if (date.getDay() != null) {
					sumoTerms.add("day(time-"+timeCount+","+date.getDay()+")");
				}
				if (date.getMonth() != null) {
					sumoTerms.add("month(time-"+timeCount+","+date.getMonth()+")");
				}
				if (date.getYear() != null) {
					sumoTerms.add("year(time-"+timeCount+","+date.getYear()+")");
				}
				String tokenRoot = getRootWord(date.getWordIndex());
				date.setTimeCount(timeCount);
				if (tokenRoot != null) {				
					sumoTerms.add("time("+tokenRoot+","+"time-"+timeCount+")");
				}
				timeCount++;
			}
		}
		return dateList;
	}

	/** ***************************************************************
	 */
	private void generateSumoTimeTerms(List<TimeInfo> timesList) {

		for (TimeInfo times : timesList) {
			if ((times.getSecond() != null) || (times.getMinute() != null) || (times.getHour() != null)) {
				//StringBuffer timeFn = new StringBuffer();
				if (times.getSecond() != null) {
					sumoTerms.add("second("+"time-"+timeCount+","+times.getSecond()+")");
				}
				if (times.getMinute() != null) {
					sumoTerms.add("minute("+"time-"+timeCount+","+times.getMinute()+")");
				}
				if (times.getHour() != null) {
					sumoTerms.add("hour("+"time-"+timeCount+","+times.getHour()+")");
				}
				String tokenRoot = getRootWord(times.getWordIndex());
				if (tokenRoot != null) {
					sumoTerms.add("time("+tokenRoot+","+"time-"+timeCount+")");
				}
				timeCount++;
			}
		}
	}

	/** ***************************************************************
	 */

	private void measureFn(Tokens token, int count) {

		IndexedWord tokenNode = StanfordDependencies.getNodeByIndex(token.getId());
		IndexedWord unitOfMeasurementNode = StanfordDependencies.getParent(tokenNode);
		IndexedWord measuredEntity = null;
		String posTagRemover = null;
		String unitOfMeasurementStr = "";
		List<String> visitedNodes = new ArrayList<String>();
		Matcher posTagRemoverMatcher = null;
		String measuredEntityStr = null;
		boolean flag = false;
		int x = 0;
		if (unitOfMeasurementNode != null) {
			unitOfMeasurementStr = unitOfMeasurementNode.value();
			measuredEntity = StanfordDependencies.getParent(unitOfMeasurementNode);
			visitedNodes.add(unitOfMeasurementNode.toString()+"-"+unitOfMeasurementNode.index());
		}
		if ((measuredEntity == null) && (unitOfMeasurementNode != null)) {
			for (SemanticGraphEdge e : StanfordDependencies.getOutEdgesSorted(unitOfMeasurementNode)) {
				if ((e.getRelation().toString().equals("nsubj")) || (e.getRelation().toString().equals("dobj"))) {
					measuredEntity = e.getDependent();
					flag = true;
					break;
				}
			}
		}
		else if ((measuredEntity == null) && (unitOfMeasurementNode == null)){
			return;
		}
		while ((measuredEntity != null) && (!flag)) {
			measuredEntityStr = measuredEntity.value()+"-"+measuredEntity.index();
			if (!visitedNodes.contains(measuredEntityStr)) {
				visitedNodes.add(measuredEntityStr);
			}
			posTagRemoverMatcher = POS_TAG_REMOVER.matcher(measuredEntity.toString());
			if(posTagRemoverMatcher.find()) {
				posTagRemover = posTagRemoverMatcher.group(1);
				System.out.println(posTagRemover);
				if(nounTags.contains(posTagRemover)) {
					break;
				}
				//IndexedWord tempMeasuredEntity = StanfordDependencies.getParent(measuredEntity);
				if (StanfordDependencies.getParent(measuredEntity) == null) {
					Set<IndexedWord> childrenSet = StanfordDependencies.getChildren(measuredEntity);
					for (IndexedWord child : childrenSet) {
						String childPosTagRemover = null;
						System.out.println(child.toString());
						posTagRemoverMatcher = POS_TAG_REMOVER.matcher(child.toString());
						//childPosTagRemover = posTagRemoverMatcher.group(1);
						if (posTagRemoverMatcher.find()) {
							childPosTagRemover = posTagRemoverMatcher.group(1); 
						}
						if (!(visitedNodes.contains(child.toString()+"-"+child.index())) && (nounTags.contains(childPosTagRemover.replaceFirst("\\/", "")))){
							measuredEntity = child;
							visitedNodes.add(child.toString()+"-"+child.index());
							flag = true;
							break;
						}
					}
				}
				else {
					measuredEntity = StanfordDependencies.getParent(measuredEntity);
				}
			}
		}
		if (measuredEntity != null) {
			sumoTerms.add("measure(" + measuredEntity.value() + "-" + measuredEntity.index() + ", measure" + count + ")");
		}
		String sumoUnitOfMeasure = WSD.getBestDefaultSUMOsense(unitOfMeasurementNode.value(), 1);
		System.out.println(sumoUnitOfMeasure);
		if (!sumoUnitOfMeasure.isEmpty()) {
			sumoUnitOfMeasure = sumoUnitOfMeasure.replaceAll("[^\\p{Alpha}\\p{Digit}]+","");
		}
		else 
		{
			sumoUnitOfMeasure = unitOfMeasurementStr;
		}
		sumoTerms.add("unit(measure" + count + ", "+ sumoUnitOfMeasure + ")");
		sumoTerms.add("value(measure" + count + ", " + token.getWord() + ")");
		System.out.println(unitOfMeasurementStr);
		System.out.println(measuredEntityStr);
		WordNet.wn.initOnce();
		System.out.println();
	}
	/** ***************************************************************
	 */
	public List<String> getMeasureTerms() {
		return sumoTerms;
	}
	/** ***************************************************************
	 */
	public void setMeasureTerms(List<String> measureTerms) {
		this.measureTerms = measureTerms;
	}

	/** ***************************************************************
	 */
	private boolean containsIndexWord(String word) {

		for (String verbTag: VerbTags) {
			if (verbTag.contains(word)) {
				return true;
			}
		}
		return false;
	}
	/** ***************************************************************
	 */
	private String populateRootWord(int wordIndex) {

		IndexedWord tempParent = StanfordDependencies.getNodeByIndex(wordIndex);
		while (!tempParent.equals(StanfordDependencies.getFirstRoot())) {
			tempParent = StanfordDependencies.getParent(tempParent);
			if (containsIndexWord(tempParent.tag())) {
				return tempParent.word();
			}
		}
		return null;
	}
	/** ***************************************************************
	 */
	private String getRootWord(int dateId) {

		return populateRootWord(dateId);

	}

	/** ***************************************************************
	 */
	private List<TimeInfo> processTime(List<String> tokenIdNormalizedTimeMap) {

		List<TimeInfo> timesList = new ArrayList<TimeInfo>();
		for (String timeToken : tokenIdNormalizedTimeMap) {
			int id = Integer.valueOf(timeToken.split("@")[0]);
			String timeStr = timeToken.split("@")[1];
			Matcher hourMinPatternMatcher = HOUR_MINUTE_PATTERN.matcher(timeStr);
			Matcher hourMinSecPatternMatcher = HOUR_MINUTE_SECOND_PATTERN.matcher(timeStr);
			Matcher yearMonthTimePatternMatcher = YEAR_MONTH_TIME_PATTERN.matcher(timeStr);
			TimeInfo timeObj = new TimeInfo();
			if (hourMinPatternMatcher.find() && (StanfordDependencies.getNodeByIndexSafe(id)!=null)) {
				timeObj.setMinute(hourMinPatternMatcher.group(2));
				timeObj.setHour(hourMinPatternMatcher.group(1));
				timeObj.setWordIndex(id);
			} else if (hourMinSecPatternMatcher.find() && (StanfordDependencies.getNodeByIndexSafe(id)!=null)) {
				timeObj.setMinute(hourMinSecPatternMatcher.group(2));
				timeObj.setHour(hourMinSecPatternMatcher.group(1));
				timeObj.setSecond(hourMinSecPatternMatcher.group(3));
				timeObj.setWordIndex(id);
			} else if (yearMonthTimePatternMatcher.find() && (StanfordDependencies.getNodeByIndexSafe(id)!=null)) {
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
				timeObj.setWordIndex(id);
				if (yearMonthTimePatternMatcher.group(6) != null) {
					timeObj.setSecond(yearMonthTimePatternMatcher.group(6));
					timeObj.setWordIndex(id);
				}

			}
			if (!containsTimeInfo(timesList,timeObj)) {
				timesList.add(timeObj);
			}	
		}
		return timesList;
	}
	/** ***************************************************************
	 */
	public boolean containsTimeInfo(List<TimeInfo> timeList, TimeInfo timeObject) {
		for (TimeInfo t : timeList) {
			if (t.equals(timeObject)) {
				return true;
			}
		}
		return false;
	}
	/** ***************************************************************
	 */
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
	private void filterSumoTerms() {
	
		Set<String> hashsetList = new HashSet<String>(sumoTerms);
		sumoTerms.clear();
		sumoTerms.addAll(hashsetList);
	}
	
	/** ***************************************************************
	 */
	private void handleDurations() {
		
		for(int i = 0; i < allDatesList.size() - 1; i++) {
			if((allDatesList.get(i).getEndIndex() + 2) == (allDatesList.get(i + 1).getWordIndex())) {
				//System.out.println("Duration consists of ::");
				//allDatesList.get(i).print();
				//allDatesList.get(i+1).print();
				IndexedWord tempParent = StanfordDependencies.getNodeByIndex(allDatesList.get(i).getWordIndex());	
				while (!tempParent.equals(StanfordDependencies.getFirstRoot())) {
					tempParent = StanfordDependencies.getParent(tempParent);
					if (VerbTags.contains(tempParent.tag()) ||
							nounTags.contains(tempParent.tag())) {
						break;
					}
				}
				if(tempParent != null) {
					//System.out.println("Duration is associated with ::" + tempParent);
					if(VerbTags.contains(tempParent.tag())) {
						sumoTerms.add("StartTime(" + tempParent.value() + "," + "time-" + allDatesList.get(i).getTimeCount() + ")");
						sumoTerms.add("EndTime(" + tempParent.value() + "," + "time-" + allDatesList.get(i+1).getTimeCount() + ")");
					}
					if(nounTags.contains(tempParent.tag())) {
						if(tempParent.ner().equals("PERSON")) {
							sumoTerms.add("BirthDate(" + tempParent.value() + "," + "time-" + allDatesList.get(i).getTimeCount() + ")");
							sumoTerms.add("DeathDate(" + tempParent.value() + "," + "time-" + allDatesList.get(i+1).getTimeCount() + ")");
						} else {
							sumoTerms.add("StartTime(" + tempParent.value() + "," + "time-" + allDatesList.get(i).getTimeCount() + ")");
							sumoTerms.add("EndTime(" + tempParent.value() + "," + "time-" + allDatesList.get(i+1).getTimeCount() + ")");
						}
					}
				}
			}
		}
	}
	
	/** ***************************************************************
	 */
	public List<String> generateSumoTerms(List<Tokens> tokensList, StanfordDateTimeExtractor stanfordParser) {

		this.StanfordDependencies = stanfordParser.getDependencies();
		List<String> tokenIdNormalizedTimeMap = new ArrayList<String>();
		int numberCount = 1;
		for(Tokens token : tokensList) {
			switch(token.getNer()) {
				case "DATE"  : processDate(token, stanfordParser.getDependencyList());
							   break;
				case "NUMBER":
				case "PERCENT" : measureFn(token,numberCount); ++numberCount;  //processNumber(token,stanfordParser.getDependencyList());
								 break;
				case "DURATION" : break;
				case "TIME" : tokenIdNormalizedTimeMap.add(token.getId() + "@" + token.getNormalizedNer());
			}
		}
		List<TimeInfo> timesList = processTime(tokenIdNormalizedTimeMap);
		generateSumoDateTerms();
		handleDurations();
		generateSumoTimeTerms(timesList);
		filterSumoTerms();
		return sumoTerms;
	}
}
