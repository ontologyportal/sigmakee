package com.articulate.sigma.semRewrite.datesandnumber;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;

public class Utilities {
	HashMap<Integer,String> dateMap = new LinkedHashMap<Integer,String>();
	
	public static final List<String> MONTHS = new ArrayList<String>(Arrays.asList("january",
			"february","march","april","may","june","july","august",
			"september","october","november","december"));
	public static final List<String> DAYS = new ArrayList<String>(Arrays.asList("monday",
			"tuesday","wednesday","thursday","friday","saturday","sunday"));
	public static final List<String> VerbTags = new ArrayList<String>(Arrays.asList("VB",
			"VBD","VBG","VBN","VBP","VBZ"));
	public static final List<String> nounTags = new ArrayList<String>(Arrays.asList("NN","NNS","NNP","NNPS","/NN","/NNS","/NNP", "/NNPS"));
	
	List<String> sumoTerms = new LinkedList<String>();
	List<DateInfo> allDatesList = new LinkedList<DateInfo>();
	SemanticGraph StanfordDependencies;
	int timeCount = 1;
	
	boolean containsIndexWord(String word) {

		for (String verbTag: VerbTags) {
			if (verbTag.contains(word)) {
				return true;
			}
		}
		return false;
	}

	String populateRootWord(int wordIndex) {

		IndexedWord tempParent = StanfordDependencies.getNodeByIndex(wordIndex);
		while (!tempParent.equals(StanfordDependencies.getFirstRoot())) {
			tempParent = StanfordDependencies.getParent(tempParent);
			if (containsIndexWord(tempParent.tag())) {
				return tempParent.word()+"-"+tempParent.index();
			}
		}
		return null;
	}
	/** ***************************************************************
	 */
	String getRootWord(int dateId) {

		//System.out.println("Id is ::" + dateId);
		return populateRootWord(dateId);

	}
	
	 void filterSumoTerms() {
		
		Set<String> hashsetList = new HashSet<String>(sumoTerms);
		sumoTerms.clear();
		sumoTerms.addAll(hashsetList);
		//List<String> removableList = new ArrayList<String>();
		Set<String> removableSumoTerms = new HashSet<String>();
		for (DateInfo d : allDatesList) {
			if (d.isDuration()) {
				//removableList.add("time-"+d.getTimeCount());
				for(String sumoTerm : sumoTerms) {
					if(sumoTerm.matches("^time\\(.*,time-"+d.getTimeCount()+"\\)$")) {
						removableSumoTerms.add(sumoTerm);
					}
				}
			}
		}
		sumoTerms.removeAll(removableSumoTerms);
	}
}
