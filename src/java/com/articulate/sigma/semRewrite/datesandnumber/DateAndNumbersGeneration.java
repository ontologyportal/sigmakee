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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.articulate.sigma.WSD;
import com.articulate.sigma.WordNet;
import com.articulate.sigma.semRewrite.ClauseGroups;

import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;

public class DateAndNumbersGeneration {

	static enum DateComponent {
		DAY, MONTH, YEAR
	}
	
	static final Pattern HOUR_MINUTE_PATTERN = Pattern.compile("^T([0-9]{2}):([0-9]{2})$");
	static final Pattern HOUR_MINUTE_SECOND_PATTERN = Pattern.compile("^T([0-9]{2}):([0-9]{2}):([0-9]{2})$");
	static final Pattern YEAR_MONTH_TIME_PATTERN = Pattern.compile("^([0-9X]{4})(\\-[0-9]{2})?(\\-[0-9]{2})?T([0-9]{2}):([0-9]{2})(:[0-9]{2})?");
	static final Pattern POS_TAG_REMOVER = Pattern.compile("(\\/[A-Z]+)$");

	List<String> measureTerms = new ArrayList<String>();
	
	/** ***************************************************************
	 */
	public DateAndNumbersGeneration() {
	}
	
	/** ***************************************************************
	 */
	private void generateSumoTimeTerms(List<TimeInfo> timesList, Utilities utilities) {

		for (TimeInfo times : timesList) {
			if ((times.getSecond() != null) || (times.getMinute() != null) || (times.getHour() != null)) {
				//StringBuffer timeFn = new StringBuffer();
				if (times.getSecond() != null) {
					utilities.sumoTerms.add("second("+"time-"+utilities.timeCount+","+times.getSecond()+"-"+times.getWordIndex() +")");
				}
				if (times.getMinute() != null) {
					utilities.sumoTerms.add("minute("+"time-"+utilities.timeCount+","+times.getMinute()+"-"+times.getWordIndex()+")");
				}
				if (times.getHour() != null) {
					utilities.sumoTerms.add("hour("+"time-"+utilities.timeCount+","+times.getHour()+"-"+times.getWordIndex()+")");
				}
				String tokenRoot = utilities.getRootWord(times.getWordIndex());
				if (tokenRoot != null) {
					utilities.sumoTerms.add("time("+tokenRoot+","+"time-"+utilities.timeCount+")");
				}
				utilities.timeCount++;
			}
		}
	}

	/** ***************************************************************
	 */
	private void measureFn(Tokens token, int count, Utilities utilities) {

		IndexedWord tokenNode = utilities.StanfordDependencies.getNodeByIndex(token.getId());
		IndexedWord unitOfMeasurementNode = utilities.StanfordDependencies.getParent(tokenNode);
		IndexedWord measuredEntity = null;
		String posTagRemover = null;
		String unitOfMeasurementStr = "";
		String sumoUnitOfMeasure = "";
		List<String> visitedNodes = new ArrayList<String>();
		Matcher posTagRemoverMatcher = null;
		String measuredEntityStr = null;
		boolean flag = false;
		//int x = 0;
		if (unitOfMeasurementNode != null) {
			unitOfMeasurementStr = unitOfMeasurementNode.value();
			measuredEntity = utilities.StanfordDependencies.getParent(unitOfMeasurementNode);
			visitedNodes.add(unitOfMeasurementNode.toString()+"-"+unitOfMeasurementNode.index());
		}
		if ((measuredEntity == null) && (unitOfMeasurementNode != null)) {
			for (SemanticGraphEdge e : utilities.StanfordDependencies.getOutEdgesSorted(unitOfMeasurementNode)) {
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
				if(Utilities.nounTags.contains(posTagRemover)) {
					break;
				}
				//IndexedWord tempMeasuredEntity = StanfordDependencies.getParent(measuredEntity);
				if (utilities.StanfordDependencies.getParent(measuredEntity) == null) {
					Set<IndexedWord> childrenSet = utilities.StanfordDependencies.getChildren(measuredEntity);
					//which means it is unitOfMeasurementNode. Hence remove infinite looping condition
					if ((childrenSet.size()==1)) {
						measuredEntity = unitOfMeasurementNode;
						utilities.sumoTerms.add("measure(" + measuredEntity.value() + "-" + measuredEntity.index() + ", measure" + count + ")");
						utilities.sumoTerms.add("unit(measure" + count + ", "+ "memberCount" + ")");
						utilities.sumoTerms.add("value(measure" + count + ", " + token.getWord()+ ")");
						utilities.sumoTerms.add("valueToken("+token.getWord()+","+token.getWord()+"-"+token.getId()+")");
						flag = true;
						return;
					}
					IndexedWord measuredEntity_temp = null;
					for (IndexedWord child : childrenSet) {
						String childPosTagRemover = null;
						posTagRemoverMatcher = POS_TAG_REMOVER.matcher(child.toString());
						//childPosTagRemover = posTagRemoverMatcher.group(1);
						if (posTagRemoverMatcher.find()) {
							childPosTagRemover = posTagRemoverMatcher.group(1); 
						}
						if (!(visitedNodes.contains(child.toString()+"-"+child.index())) && (Utilities.nounTags.contains(childPosTagRemover.replaceFirst("\\/", "")))){
							if ((utilities.StanfordDependencies.reln(measuredEntity, child) != null) && (utilities.StanfordDependencies.reln(measuredEntity, child).getShortName().equals("nsubj"))) {
								measuredEntity = child;
								visitedNodes.add(child.toString()+"-"+child.index());
								flag = true;
								break;
							}
							measuredEntity_temp = child;
							visitedNodes.add(child.toString()+"-"+child.index());
						}
					}
					if (!flag) {
						measuredEntity = measuredEntity_temp;
						flag = true; 
					}
					
				}
				else {
					measuredEntity = utilities.StanfordDependencies.getParent(measuredEntity);
				}
			}
		}
		if (measuredEntity != null) {
			utilities.sumoTerms.add("measure(" + measuredEntity.value() + "-" + measuredEntity.index() + ", measure" + count + ")");
		}
		sumoUnitOfMeasure = WSD.getBestDefaultSUMOsense(unitOfMeasurementNode.value(), 1);
		if ((sumoUnitOfMeasure != null) && (!sumoUnitOfMeasure.isEmpty())) {
			sumoUnitOfMeasure = sumoUnitOfMeasure.replaceAll("[^\\p{Alpha}\\p{Digit}]+","");
		}
		else 
		{
			if ((measuredEntity != null) && (unitOfMeasurementStr.equals(measuredEntity.value()))) {
				unitOfMeasurementStr = "memberCount";
			}
			sumoUnitOfMeasure = unitOfMeasurementStr;
		}
		utilities.sumoTerms.add("unit(measure" + count + ", "+ sumoUnitOfMeasure + ")");
		utilities.sumoTerms.add("value(measure" + count + ", " + token.getWord() + ")");
		utilities.sumoTerms.add("valueToken("+token.getWord()+","+token.getWord()+"-"+token.getId()+")");
		WordNet.wn.initOnce();
	}
	
	/** ***************************************************************
	 */
	public List<String> getMeasureTerms(Utilities utilities) {
		
		return utilities.sumoTerms;
	}
	
	/** ***************************************************************
	 */
	public void setMeasureTerms(List<String> measureTerms) {
		
		this.measureTerms = measureTerms;
	}

	/** ***************************************************************
	 */
	private List<TimeInfo> processTime(List<String> tokenIdNormalizedTimeMap, Utilities utilities) {

		List<TimeInfo> timesList = new ArrayList<TimeInfo>();
		for (String timeToken : tokenIdNormalizedTimeMap) {
			int id = Integer.valueOf(timeToken.split("@")[0]);
			String timeStr = timeToken.split("@")[1];
			Matcher hourMinPatternMatcher = HOUR_MINUTE_PATTERN.matcher(timeStr);
			Matcher hourMinSecPatternMatcher = HOUR_MINUTE_SECOND_PATTERN.matcher(timeStr);
			Matcher yearMonthTimePatternMatcher = YEAR_MONTH_TIME_PATTERN.matcher(timeStr);
			TimeInfo timeObj = new TimeInfo();
			if (hourMinPatternMatcher.find() && (utilities.StanfordDependencies.getNodeByIndexSafe(id)!=null)) {
				timeObj.setMinute(hourMinPatternMatcher.group(2));
				timeObj.setHour(hourMinPatternMatcher.group(1));
				timeObj.setWordIndex(id);
			} 
			else if (hourMinSecPatternMatcher.find() && (utilities.StanfordDependencies.getNodeByIndexSafe(id)!=null)) {
				timeObj.setMinute(hourMinSecPatternMatcher.group(2));
				timeObj.setHour(hourMinSecPatternMatcher.group(1));
				timeObj.setSecond(hourMinSecPatternMatcher.group(3));
				timeObj.setWordIndex(id);
			} 
			else if (yearMonthTimePatternMatcher.find() && (utilities.StanfordDependencies.getNodeByIndexSafe(id)!=null)) {
				String year = yearMonthTimePatternMatcher.group(1);
				int tokenCnt = new StanfordDateTimeExtractor().getTokenCount() + 1;
				if (!year.equals("XXXX")) {
					if(!checkValueInMap("YEAR@" + year,utilities)) {
						utilities.dateMap.put(id, "YEAR@" + year);
					}
				}
				if (yearMonthTimePatternMatcher.group(2) != null) {
					String month = yearMonthTimePatternMatcher.group(2).replaceAll("\\-", "");
					if(!checkValueInMap("MONTH@" + month,utilities) && !checkValueInMap("MONTH@" + Utilities.MONTHS.get(Integer.valueOf(month) - 1),utilities)) {
						utilities.dateMap.put(tokenCnt + id, "MONTH@" + Utilities.MONTHS.get(Integer.valueOf(month) - 1));
						tokenCnt ++;
					}
				}
				if (yearMonthTimePatternMatcher.group(3) != null) {
					String day = yearMonthTimePatternMatcher.group(3).replaceAll("\\-", "");
					if(!checkValueInMap("DAYS@" + day,utilities)) {
						utilities.dateMap.put(tokenCnt + id, "DAYS@" + day);
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
	private boolean checkValueInMap(String value, Utilities utilities) {
		
		for (int key : utilities.dateMap.keySet()) {
			if (utilities.dateMap.get(key).equalsIgnoreCase(value)) {
				return true;
			}
		}
		return false;
	}
		
	/** ***************************************************************
	 */
	public List<String> generateSumoTerms(List<Tokens> tokensList, StanfordDateTimeExtractor stanfordParser, ClauseGroups cg) {

		DatesAndDuration datesandDurationHandler = new DatesAndDuration();
		Utilities utilities = new Utilities();
		utilities.StanfordDependencies = stanfordParser.getDependencies();
		List<String> tokenIdNormalizedTimeMap = new ArrayList<String>();
		int numberCount = 1;
		for(Tokens token : tokensList) {
			switch(token.getNer()) {
				case "DATE"  : datesandDurationHandler.processDate(token, stanfordParser.getDependencyList(),utilities);
							   break;
				case "NUMBER":
				case "ORDINAL":
				case "PERCENT" : measureFn(token,numberCount, utilities); ++numberCount;  //processNumber(token,stanfordParser.getDependencyList());
								 break;
				case "DURATION" : datesandDurationHandler.processDuration(token,utilities); break;
				case "TIME" : tokenIdNormalizedTimeMap.add(token.getId() + "@" + token.getNormalizedNer());
			}
		}
		List<TimeInfo> timesList = processTime(tokenIdNormalizedTimeMap,utilities);
		datesandDurationHandler.generateSumoDateTerms(utilities);
		datesandDurationHandler.handleDurations(utilities);
		generateSumoTimeTerms(timesList,utilities);
		utilities.filterSumoTerms(cg);
		return utilities.sumoTerms;
	}
}
