package com.articulate.sigma;

import java.util.ArrayList;
import java.util.List;

import edu.uci.ics.jung.graph.Tree;

/**
 * This class extracts personal attributes using generic pattern trees.
 *
 */
public class PersonAttExtractor {

	/** threshold 0.5 by default */
	public double mThreshold = 0.5;
	
	public PersonAttExtractor(){
	}
	
	public void setThreshold(double threshold){
		mThreshold = threshold;
	}
	public double getThreshold(){
		return mThreshold;
	}
	
	public List<Tree<ParseTreeNode, ParseTreeEdge>> extract(String patternTreeFile, String candidateTreeFile, String triggerWord){
		TreeBuilder tb = new TreeBuilder();
		List<Tree<ParseTreeNode, ParseTreeEdge>> patternTrees = tb.buildPatternTrees(patternTreeFile, triggerWord);
		List<Tree<ParseTreeNode, ParseTreeEdge>> candidateTrees = tb.buildCandidateTrees(candidateTreeFile, triggerWord);
		
		TreeKernel tk = new TreeKernel(true);
		List<Tree<ParseTreeNode, ParseTreeEdge>> validTrees = new ArrayList<Tree<ParseTreeNode, ParseTreeEdge>>();
		for(Tree<ParseTreeNode, ParseTreeEdge> candidate : candidateTrees){
			
			for(Tree<ParseTreeNode, ParseTreeEdge> pattern : patternTrees){
				double similarity = tk.treeKernelSim(candidate, pattern);
				
				if(similarity > 0){
					System.out.println(similarity);
					validTrees.add(candidate);
				}
					
			}
		}
		
		return validTrees;
	}
	
	public static void main(String[] args){
		PersonAttExtractor pae = new PersonAttExtractor();
		String patternTreeFile = "data/pattern.xml";
		String candidateTreeFile = "data/candidates.xml";
		String triggerWord = "brother";
		List<Tree<ParseTreeNode, ParseTreeEdge>> validTrees = pae.extract(patternTreeFile, candidateTreeFile, triggerWord);
		
	}
}
