package com.articulate.sigma;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.collections15.Factory;

import edu.stanford.nlp.trees.Tree;
import edu.uci.ics.jung.graph.DelegateForest;
import edu.uci.ics.jung.graph.Forest;

public class TreeBuilder {

	public String mTreeFile = null;
	private int mNodeCount = 1;
	
	public TreeBuilder(String treeFile){
		mTreeFile = treeFile;
	}
	
	
	Factory<Integer> edgeFactory = new Factory<Integer>() {
		int i = 0;

		public Integer create() {
			return i++;
		}
	};
	
	public void buildTrees(){
		SentenceParser sp = new SentenceParser(mTreeFile);
		ArrayList<SentenceInstance> instances = sp.loadSentenceInstances();
		ArrayList<Tree> parseTrees = sp.loadParseTrees();
		
		for(int i=0; i<instances.size(); i++){
			Tree parse = parseTrees.get(i);
			SentenceInstance instance = instances.get(i);
			augmentParseTree(parse, instance);
		}
	}
	/**
	 * Count the number of nodes.
	 * @param parse
	 * @param count
	 */
	private void countNodes(Tree parse){
		Tree[] children = parse.children();
		if(children.length > 0){
			mNodeCount += children.length;
			
			for(Tree child : children){
				countNodes(child);				
			}
		}	
	}
	
	/**
	 * Collapse the entity nodes, augument other nodes with lemma, POS
	 * @param parse
	 * @param instance
	 */
	public void augmentParseTree(Tree parse, SentenceInstance instance){
		mNodeCount = 1;
		countNodes(parse);
		
		Tree root = getRoot(parse);
		Forest <ParseTreeNode, ParseTreeEdge> forest = new DelegateForest <ParseTreeNode, ParseTreeEdge>();
		ParseTreeNode parent = new ParseTreeNode(root.value(), mNodeCount);
		forest.addVertex(parent);
		
		growTree(root, parent, forest);
		
		
		edu.uci.ics.jung.graph.Tree<ParseTreeNode, ParseTreeEdge> tt = forest.getTrees().iterator().next();
		Collection<ParseTreeNode> childs = tt.getVertices();
		Map<Integer, String> map = new HashMap<Integer, String>();
		for(ParseTreeNode node : childs){
			if(node.isLeaf()){
				
			}
				
		}
	}
	/**
	 * Get the root node of the tree.
	 * @param t
	 * @return
	 */
	private Tree getRoot(Tree t){
		return t.getChildrenAsList().get(0);
	}
	
	
	private void growTree(Tree root, ParseTreeNode parent, Forest <ParseTreeNode, ParseTreeEdge> forest){
		Tree[] children = root.children();
		if( children.length > 0 ){
			for( Tree ch : children ){
				mNodeCount ++;
				
				ParseTreeNode child = new ParseTreeNode(ch.value(), mNodeCount);
				if(ch.isLeaf())
					child.setAsLeaf(true);
				
				ParseTreeEdge edge = new ParseTreeEdge(edgeFactory.create(), parent, child);
				forest.addEdge(edge, parent, child);
				
				growTree(ch, child, forest);
			}
		}
		
		
		
	}
	
	/**
	 * Create a tree node and assign lemma, ner type to the leaf node
	 * @param t
	 * @param index
	 * @param instance
	 * @return
	 */
	public ParseTreeNode createTreeNode(Tree t, int index, SentenceInstance instance){
		ParseTreeNode node = new ParseTreeNode(t.value(), index);
		
		if(t.isLeaf()){
			node.setAsLeaf(true);
			
			String key = t.value() + "_" + index;
			if(instance.getWords2Lemma().containsKey(key)){
				String lemma = instance.getWords2Lemma().get(key);
				node.setNodeLemma(lemma);
			}
			
			if(instance.getWords2NER().containsKey(key)){
				String ner = instance.getWords2NER().get(key);
				node.setNodeNER(ner);
			}
		}
		
		return node;
	}
	
	
	public static void main(String[] args){
		String treeFile = "data/output4.xml";
		TreeBuilder tb = new TreeBuilder(treeFile);
		tb.buildTrees();
	}
	
	
}
