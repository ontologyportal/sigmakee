package com.articulate.sigma;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

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
	
	public List<Tree<ParseTreeNode, ParseTreeEdge>> extract(String patternTreeFile, String candidateTreeFile, String triggerWord, String outFile) throws Exception{
		
		PrintWriter writer = FileUtils.getWriter(outFile);
		TreeBuilder tb = new TreeBuilder();
		Map<edu.uci.ics.jung.graph.Tree<ParseTreeNode, ParseTreeEdge>, SentenceInstance> patternTrees = tb.buildPatternTrees(patternTreeFile, triggerWord);
		Map<edu.uci.ics.jung.graph.Tree<ParseTreeNode, ParseTreeEdge>, SentenceInstance> candidateTrees = tb.buildCandidateTrees(candidateTreeFile, triggerWord);
		
		TreeKernel tk = new TreeKernel(true);
		List<Tree<ParseTreeNode, ParseTreeEdge>> validTrees = new ArrayList<Tree<ParseTreeNode, ParseTreeEdge>>();
		int i = 1;
		for(Tree<ParseTreeNode, ParseTreeEdge> candidate : candidateTrees.keySet()){
			
			Tree<ParseTreeNode, ParseTreeEdge> bestPattern = null;
			double bestSim = Double.MIN_VALUE;
			for(Tree<ParseTreeNode, ParseTreeEdge> pattern : patternTrees.keySet()){
				double similarity = tk.treeKernelSim(candidate, pattern);
				
				if(similarity > bestSim){
					bestSim = similarity;
					bestPattern = pattern;
				}	
			}
			
			if( bestPattern != null ){
				System.out.println(i + "\tsim: " + bestSim);
				SentenceInstance patternInst = patternTrees.get(bestPattern);
				writer.write("<match>\n    <pattern sourceEntity=\"" + patternInst.getSourceEntity() + "\" targetEntity=\"" + patternInst.getTargetEntity() + "\">");
				String patRet = sortLeaves(bestPattern)+ "</pattern>\n";
				writer.write(patRet);
				
				SentenceInstance candidateInst = patternTrees.get(candidate);
				String candVal = candidateInst.getSourceEntity() + "\t" + candidateInst.getTargetEntity();
				writer.write("    <candidate sourceEntity=\"" + candidateInst.getSourceEntity() + "\" targetEntity=\"" + candidateInst.getTargetEntity() + "\">");
				String canRet = sortLeaves(candidate) + "</candidate>\n";
				writer.write(patRet);
				
				writer.write("    <similarity>" + bestSim + "</similarity>\n</match>\n");
				
				writer.flush();
				i++;
			}
			
		}
		writer.flush();
		writer.close();
		return validTrees;
	}
	
	public String sortLeaves(Tree<ParseTreeNode, ParseTreeEdge> tree){
		Map<Integer, String> map = new HashMap<Integer, String>();
		for( ParseTreeNode node : tree.getVertices() ){
			if(node.isLeaf())
				map.put(node.getNodeIndex(), node.getNodeText());
		}
		SortedSet<Integer> sort = new TreeSet<Integer>(map.keySet());
		String ret = "";
		for(Integer i : sort){
			ret += map.get(i) + " ";
		}
		return ret.trim();
	}
	
	public static void main(String[] args)throws Exception{
		PersonAttExtractor pae = new PersonAttExtractor();
		String patternTreeFile = "data/pattern.xml";
		String candidateTreeFile = "data/candidates.xml";
		String triggerWord = "brother";
		String outfile = "data/result.xml";
		pae.extract(patternTreeFile, candidateTreeFile, triggerWord, outfile);
		
	}
}
