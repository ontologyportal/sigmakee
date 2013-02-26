package com.articulate.sigma;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.collections15.Factory;

import edu.stanford.nlp.trees.Tree;
import edu.uci.ics.jung.graph.DelegateForest;
import edu.uci.ics.jung.graph.Forest;
import edu.uci.ics.jung.graph.util.TreeUtils;

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
		ArrayList<edu.uci.ics.jung.graph.Tree<ParseTreeNode, ParseTreeEdge>> augmentTrees 
			= new ArrayList<edu.uci.ics.jung.graph.Tree<ParseTreeNode, ParseTreeEdge>>();
		
		for(int i=0; i<instances.size(); i++){
			Tree parse = parseTrees.get(i);
			SentenceInstance instance = instances.get(i);
			edu.uci.ics.jung.graph.Tree<ParseTreeNode, ParseTreeEdge> augmentedTree = augmentParseTree(parse, instance);
			String[] str = new String[]{"Sarah", "Joe"};
			augmentedTree = pruneTree(augmentedTree, str);
			
			System.out.println(augmentedTree.toString());
			TreeKernel tk = new TreeKernel(true);
			double sim = tk.treeKernelSim(augmentedTree, augmentedTree);
			System.out.println(sim);
//			augmentTrees.add(augmentedTree);
		}
		
		
	}
	
	/**
	 * Prune the tree by specifying the leaf nodes.
	 * @param tree
	 * @param leafTexts
	 * @return
	 */
	private edu.uci.ics.jung.graph.Tree<ParseTreeNode, ParseTreeEdge> pruneTree(edu.uci.ics.jung.graph.Tree<ParseTreeNode, ParseTreeEdge> tree, String[] leafTexts){
		
		edu.uci.ics.jung.graph.Tree<ParseTreeNode, ParseTreeEdge> prunedTree = tree;
		try{
			Collection<ParseTreeNode> nodes = tree.getVertices();
			int height = tree.getHeight();
			
			for(ParseTreeNode node : nodes){
				if(node.isLeaf()) continue;
				
				edu.uci.ics.jung.graph.Tree<ParseTreeNode, ParseTreeEdge> subtree = TreeUtils.getSubTree(tree, node);
				if( hasLeaves(subtree, leafTexts) ){
					int subTreeHeight = subtree.getHeight();
					if(subTreeHeight < height){
						height = subTreeHeight;
						prunedTree = subtree;
					}
				}
			}
		}catch(Exception e){e.printStackTrace();}
		
		return prunedTree;
	}
	/**
	 * Check if the tree has leaves.
	 * @param tree
	 * @param leafTexts
	 * @return
	 */
	private boolean hasLeaves(edu.uci.ics.jung.graph.Tree<ParseTreeNode, ParseTreeEdge> tree, String[] leafTexts){
		ArrayList<ParseTreeNode> leaves = getLeaves(tree);
		
		boolean hasLeafs = true;
		for( String txt : leafTexts ){
			
			boolean flag = false;
			for(ParseTreeNode leaf : leaves){
				if(txt.equals(leaf.getNodeText())){
					flag = true;
					break;
				}
			}
			
			if(! flag ) {//the leaf is not found
				hasLeafs = false;
				break;
			}
		}
		
		return hasLeafs;
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
	public edu.uci.ics.jung.graph.Tree<ParseTreeNode, ParseTreeEdge> augmentParseTree(Tree parse, SentenceInstance instance){
		mNodeCount = 1;
		countNodes(parse);
		
		Tree root = getRoot(parse);
		Forest <ParseTreeNode, ParseTreeEdge> forest = new DelegateForest <ParseTreeNode, ParseTreeEdge>();
		ParseTreeNode parent = new ParseTreeNode(root.value(), mNodeCount);
		forest.addVertex(parent);
		growTree(root, parent, forest);
		
		edu.uci.ics.jung.graph.Tree<ParseTreeNode, ParseTreeEdge> agumentedTree = forest.getTrees().iterator().next();
		ArrayList<ParseTreeNode> leaves = getLeaves(agumentedTree);
		modifyLeafIndex(leaves);
		enrichLeaves(leaves, instance);
		
		return agumentedTree;
	}
	
	/**
	 * Get leaves of a tree.
	 * @param tree
	 * @return
	 */
	public ArrayList<ParseTreeNode> getLeaves(edu.uci.ics.jung.graph.Tree<ParseTreeNode, ParseTreeEdge> tree){
		ArrayList<ParseTreeNode> leaves = new ArrayList<ParseTreeNode>();
		Collection<ParseTreeNode> childs = tree.getVertices();
		for(ParseTreeNode node : childs){
			if(node.isLeaf()){
				leaves.add(node);
			}
		}
		
		return leaves;
	}
	
	/**
	 * Modify leaf node index and assign a new index starting from 1 to the number of tokens in the sentence.
	 * @param tree
	 */
	private void modifyLeafIndex(ArrayList<ParseTreeNode> leaves){
		Map<Integer, String> map = new HashMap<Integer, String>();
		for(ParseTreeNode node : leaves){
			if(node.isLeaf()){
				int nodeIndex = node.getNodeIndex();
				String nodeText = node.getNodeText();
				map.put(nodeIndex, nodeText);
			}	
		}
		
		SortedSet<Integer> sortedIndices = new TreeSet<Integer>(map.keySet());
		int newIndex = 1;
		for( int nodeIndex : sortedIndices ){
			String nodeText = map.get(nodeIndex);
			
			for(ParseTreeNode node : leaves){
				if( (node.getNodeIndex() == nodeIndex) 
						&& (node.getNodeText().equalsIgnoreCase(nodeText))){
					node.setNodeIndex(newIndex);
				}
			}
			
			newIndex ++;
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
	
	/**
	 * Grow a tree with an addition of indices
	 * @param root
	 * @param parent
	 * @param forest
	 */
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
	 * Enrich leaf nodes with lemma and NER types.
	 * @param leaves
	 * @param instance
	 */
	private void enrichLeaves(ArrayList<ParseTreeNode> leaves, SentenceInstance instance){
		
		for( ParseTreeNode leaf : leaves ){
			String key = leaf.getNodeText() + "-" + leaf.getNodeIndex();
			if(instance.getWords2Lemma().containsKey(key)){
				String lemma = instance.getWords2Lemma().get(key);
				leaf.setNodeLemma(lemma);
			}
			
			if(instance.getWords2NER().containsKey(key)){
				String ner = instance.getWords2NER().get(key);
				leaf.setNodeNER(ner);
			}
		}
	}
	
	
	public static void main(String[] args){
		String treeFile = "data/output4.xml";
		TreeBuilder tb = new TreeBuilder(treeFile);
		tb.buildTrees();
	}
	
	
}
