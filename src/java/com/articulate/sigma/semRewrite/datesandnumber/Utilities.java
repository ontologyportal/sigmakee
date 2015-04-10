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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.articulate.sigma.semRewrite.substitutor.ClauseSubstitutor;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;

public class Utilities {
    
	//HashMap<Integer,String> dateMap = new LinkedHashMap<Integer,String>();
	
	public static final List<String> MONTHS = new ArrayList<String>(Arrays.asList("january",
			"february","march","april","may","june","july","august",
			"september","october","november","december"));
	public static final List<String> DAYS = new ArrayList<String>(Arrays.asList("monday",
			"tuesday","wednesday","thursday","friday","saturday","sunday"));
	public static final List<String> VerbTags = new ArrayList<String>(Arrays.asList("VB",
			"VBD","VBG","VBN","VBP","VBZ"));
	public static final List<String> nounTags = new ArrayList<String>(Arrays.asList("NN","NNS","NNP","NNPS","/NN","/NNS","/NNP", "/NNPS"));
	
	public static final Pattern sumoTermPattern = Pattern.compile("^([a-zA-Z]+)\\(([a-zA-Z\\-0-9]+)(\\s)?,(\\s)?([a-zA-Z(\\-)?0-9]+)\\)");
	
	public static final List<String> stopWords = new ArrayList<String>(Arrays.asList("of",",","-"));
	
	List<String> sumoTerms = new LinkedList<String>();
	List<DateInfo> datesList = new LinkedList<DateInfo>();
	SemanticGraph StanfordDependencies;
	int timeCount = 1;
	
	/** ***************************************************************
     */
	public boolean containsIndexWord(String word) {

		for (String verbTag: VerbTags) {
			if (verbTag.contains(word)) {
				return true;
			}
		}
		return false;
	}

	/** ***************************************************************
     */
	public String populateRootWord(int wordIndex) {

		IndexedWord tempParent = StanfordDependencies.getNodeByIndex(wordIndex);
		while (!tempParent.equals(StanfordDependencies.getFirstRoot())) {
			tempParent = StanfordDependencies.getParent(tempParent);
			if (containsIndexWord(tempParent.tag())) {
				return tempParent.lemma()+"-"+tempParent.index();
			}
		}
		return null;
	}
	
	/** ***************************************************************
	 */
	public String getRootWord(int dateId) {

		//System.out.println("Id is ::" + dateId);
		return populateRootWord(dateId);
	}
	
	public void lemmatize(Tokens token) {
		if(!token.getPos().equals("NNP") || !token.getPos().equals("NNPS")) {
			token.setWord(token.getLemma());
		}
	}
	
	/** ***************************************************************
     */
	public void filterSumoTerms(ClauseSubstitutor substitutor) {
		
		Set<String> hashsetList = new HashSet<String>(sumoTerms);
		sumoTerms.clear();
		sumoTerms.addAll(hashsetList);
		//List<String> removableList = new ArrayList<String>();
		Set<String> removableSumoTerms = new HashSet<String>();
		for (DateInfo d : datesList) {
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
		if (substitutor != null) {
			for(int i = 0; i < sumoTerms.size(); ++i) {
				//System.out.println();
				Matcher sumoMatcher = sumoTermPattern.matcher(sumoTerms.get(i));
				if(sumoMatcher.find()) {
					String group2 = sumoMatcher.group(2);
					String group5 = sumoMatcher.group(5);
					if(!substitutor.getGrouped(group2).equals(group2)) {
						sumoTerms.set(i, sumoTerms.get(i).replace(group2, substitutor.getGrouped(group2)));
					} 
					else if (!substitutor.getGrouped(group5).equals(group5)) {
						sumoTerms.set(i, sumoTerms.get(i).replace(group5, substitutor.getGrouped(group5)));
					}
				}
			}
		}
	}
}
