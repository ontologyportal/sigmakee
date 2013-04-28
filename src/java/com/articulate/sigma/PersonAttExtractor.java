package com.articulate.sigma;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import edu.uci.ics.jung.graph.Tree;

/**
 * This class extracts personal attributes using generic pattern trees.
 *
 */
public class PersonAttExtractor {

	/** threshold 0.2 by default */
	public double mThreshold = 0.0;
	
	public PersonAttExtractor(){
	}
	
	public void setThreshold(double threshold){
		mThreshold = threshold;
	}
	public double getThreshold(){
		return mThreshold;
	}
	
	public List<Map.Entry> extract(String patternTreeFile, String candidateTreeFile, String[] triggerWords, int topN) throws Exception{
		
		TreeBuilder tb = new TreeBuilder();
		Map<edu.uci.ics.jung.graph.Tree<ParseTreeNode, ParseTreeEdge>, SentenceInstance> patternTrees = tb.buildPatternTrees(patternTreeFile, triggerWords);
		Map<edu.uci.ics.jung.graph.Tree<ParseTreeNode, ParseTreeEdge>, String> candidateTrees = tb.buildCandidateTrees(candidateTreeFile, triggerWords);
		
		TreeKernel tk = new TreeKernel(true);
		Map<String, Double> map = new HashMap<String, Double>();
		for(Tree<ParseTreeNode, ParseTreeEdge> candidate : candidateTrees.keySet()){
			
			String targetEntity = candidateTrees.get(candidate);
			
			double bestSim = Double.MIN_VALUE;
			Tree<ParseTreeNode, ParseTreeEdge> bestPattern = candidate;
			for(Tree<ParseTreeNode, ParseTreeEdge> pattern : patternTrees.keySet()){
				double similarity = tk.treeKernelSim(candidate, pattern);
				
				if(similarity > bestSim){
					bestSim = similarity;
					bestPattern = pattern;
				}	
			}
			
//			if(bestSim > 0.1){
				String can_seq = flattenTreeLeafSeq(candidate);
				String pat_seq = flattenTreeLeafSeq(bestPattern);
				System.out.println(bestSim + "\t" + targetEntity + "\t" + can_seq + "\t" + pat_seq);
//			}
			
			map.put(targetEntity, bestSim);
		}
		
		
		Map.Entry[] entries = reverseSortByDoubleValue(map);
		topN = entries.length < topN ? entries.length : topN;
		
		List<Map.Entry> list = new ArrayList<Map.Entry>();
		for(int i=0; i<topN; i++){
			Map.Entry entry = entries[i];
			list.add(entry);
		}
		
		return list;
	}
	
	/**
	 * Flatten tree sequence.
	 * @param tree
	 * @return
	 */
	public String flattenTreeLeafSeq(Tree<ParseTreeNode, ParseTreeEdge> tree){
		ArrayList<ParseTreeNode> leaves = new ArrayList<ParseTreeNode>();
		Collection<ParseTreeNode> childs = tree.getVertices();
		Map<Integer, String> map = new HashMap<Integer, String>();
		for (ParseTreeNode node : childs) {
			if (node.isLeaf()) {
				int nodeIndex = node.getNodeIndex();
				String nodeText = node.getNodeText();
				map.put(nodeIndex, nodeText);
			}
		}

		String leafSeq = "";
		SortedSet<Integer> sortedIndices = new TreeSet<Integer>(map.keySet());
		int newIndex = 1;
		for (int nodeIndex : sortedIndices) {
			String nodeText = map.get(nodeIndex);

			leafSeq += nodeText + " ";
		}
		
		return leafSeq;
	}
	
	
	public void batchExtract(String patternTreeFile, String corpusDirectory, String[] triggers, int topN, String outfile) throws Exception{
		
		PrintWriter writer = FileUtils.getWriter(outfile);
		File[] fileList = new File(corpusDirectory).listFiles();
		
		for( File file : fileList ){
			String sourceEntity = file.getName().replace(".xml", "");
			String candidateTreeFile = file.getCanonicalPath();
			List<Map.Entry> entries = extract(patternTreeFile, candidateTreeFile, triggers, topN);
			
			
			for( Map.Entry entry : entries ){
				double sim = Double.parseDouble(entry.getValue().toString());
//				System.out.println(sim);
				if( sim > mThreshold ){
					writer.write(sourceEntity + "\t" + entry.getKey().toString() + "\t" + sim + "\n");
				}
				writer.flush();
			}
		}
		writer.close();
	}
	
	/**
	 * Reverse sort of a map by value
	 * @param m
	 * @return
	 */
	public static Map.Entry[] reverseSortByDoubleValue(Map m){
	    Set set = m.entrySet();   
	    Map.Entry[] entries = (Map.Entry[]) set.toArray(new Map.Entry[set.size()]);   
	    Arrays.sort(entries, new Comparator(){
	        public int compare(Object arg0, Object arg1) {   
	        Double value1 = Double.valueOf(((Map.Entry) arg0).getValue().toString());   
	        Double value2 = Double.valueOf(((Map.Entry) arg1).getValue().toString());   
	        return -value1.compareTo(value2);//in reverse order
	        }   
	     });   
	     return entries;
	  }
	
	
	public static void main(String[] args)throws Exception{
		PersonAttExtractor pae = new PersonAttExtractor();
		String patternTreeFile = "data/pattern.xml";
		String corpusDir = "data/raw1tag";
		String triggers[] = new String[]{"brother", "brothers"};
		String outfile = "data/raw2_eval/result0.3";
		pae.setThreshold(0.3);
		int topN = 2;
		pae.batchExtract(patternTreeFile, corpusDir, triggers, topN, outfile);
		
	}
}
