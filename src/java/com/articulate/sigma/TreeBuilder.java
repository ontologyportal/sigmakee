package com.articulate.sigma;

import java.io.File;
import java.util.ArrayList;
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

	public TreeBuilder(String treeFile) {
		mTreeFile = treeFile;
	}

	public TreeBuilder(){
		
	}
	Factory<Integer> edgeFactory = new Factory<Integer>() {
		int i = 0;

		public Integer create() {
			return i++;
		}
	};

	/**
	 * Build candidate trees.
	 * @param candidateTreeFile
	 * @param triggers
	 * @return
	 */
	public Map<edu.uci.ics.jung.graph.Tree<ParseTreeNode, ParseTreeEdge>, String> buildCandidateTrees(
			String candidateTreeFile, String[] triggers) throws Exception{
		
		String sourceEntity = new File(candidateTreeFile).getName().replace(".xml", "");
		
		List<SentenceInstance> sentenceInsts = DocumentParser.loadSentences(candidateTreeFile, triggers);
		sentenceInsts = filterSentenceInstance(sentenceInsts, sourceEntity);
		List<Integer> ids = getSentenceIds(sentenceInsts);
		
		ArrayList<Tree> parseTrees = DocumentParser.loadParseTrees(candidateTreeFile);
		parseTrees = filterParseTree(parseTrees, ids);
		
		Map<edu.uci.ics.jung.graph.Tree<ParseTreeNode, ParseTreeEdge>, String> augmentTrees 
			= new HashMap<edu.uci.ics.jung.graph.Tree<ParseTreeNode, ParseTreeEdge>, String>();

		for (int i = 0; i < sentenceInsts.size(); i++) {
			Tree parse = parseTrees.get(i);
			SentenceInstance instance = sentenceInsts.get(i);
			
			List<TokenFeature> tokens = instance.getSentenceTokens();
			List<TokenFeature> names = DocumentParser.getNamedMentions(tokens, "person");
			edu.uci.ics.jung.graph.Tree<ParseTreeNode, ParseTreeEdge> augmentedTree = augmentParseTree(parse, instance);
			
			for( TokenFeature targetEntity : names ){
				
				String txt = targetEntity.getFeatureText();
				if(txt.contains(sourceEntity) || sourceEntity.contains(txt)) continue;
				
				String[] leafTexts = convert2Array(sourceEntity, txt);
				edu.uci.ics.jung.graph.Tree<ParseTreeNode, ParseTreeEdge> t = pruneTree(augmentedTree, leafTexts);
				
				if( t == null ) continue;
				if(getLeaves(t).size() > 30 || getLeaves(t).size() <= 0) continue;
				
				augmentTrees.put(t, targetEntity.getFeatureText());
			}
		}

		return augmentTrees;
	}
	/**
	 * 
	 * @param sentenceInsts
	 * @return
	 */
	public List<Integer> getSentenceIds(List<SentenceInstance> sentenceInsts){
		List<Integer> ids = new ArrayList<Integer>();
		for(SentenceInstance inst : sentenceInsts){
			ids.add(inst.getSentenceId());
		}
		
		return ids;
	}
	/**
	 * 
	 * @param parseTrees
	 */
	public ArrayList<Tree> filterParseTree(ArrayList<Tree> parseTrees, List<Integer> ids){
		
		ArrayList<Tree> trees = new ArrayList<Tree>();
		for(int i=0; i<parseTrees.size(); i++){
			int id = i + 1;
			if(ids.contains(id)){
				Tree t = parseTrees.get(i);
				trees.add(t);
			}
		}
		
		return trees;
	}
	
	/**
	 * 
	 * @param sentenceInsts
	 * @param personName
	 * @return
	 */
	public List<SentenceInstance> filterSentenceInstance(List<SentenceInstance> sentenceInsts, String personName){
		List<SentenceInstance> insts = new ArrayList<SentenceInstance>();
		for(SentenceInstance inst : sentenceInsts){
			if(isTermInSequence(inst.toString(), personName))
				insts.add(inst);
		}
		
		return insts;
	}

	/**
	 * Check if the term is in the sequence.
	 * @param sequence
	 * @param term
	 * @return
	 */
	public static boolean isTermInSequence(String sequence, String term){
		
		if(sequence == null || sequence.isEmpty() || term == null || term.isEmpty()) return false;
		
		int index = sequence.indexOf(term);
		while(index >= 0){
			//if it is an independent term, it cannot have English character preceeding or succeeding this term
			if(index == 0){//at the beginning of the sequence
				if(index + term.length() == sequence.length()){//they are of the same length
					if(sequence.equalsIgnoreCase(term)) return true;
				}else{
					char succeedingChar = sequence.charAt(index + term.length());
					if(!Character.isAlphabetic(succeedingChar))
						return true;
				}
			}else if((index + term.length()) == sequence.length()){//at the end of the sequence
				char preceedingChar = sequence.charAt( index - 1 );
				if(!Character.isAlphabetic(preceedingChar))
					return true;
			}else{//term is inbetween the sequence
				char preceedingChar = sequence.charAt( index - 1 );
				char succeedingChar = sequence.charAt(index + term.length());
				if(!Character.isAlphabetic(preceedingChar) && !Character.isAlphabetic(succeedingChar))
					return true;
			}
			
			index = sequence.indexOf(term, index + term.length());
		}
		
		return false;
	}
	
	/**
	 * Load pattern trees.
	 * 
	 * @param patternTreeFile
	 * @param triggerWord
	 * @return
	 */
	public Map<edu.uci.ics.jung.graph.Tree<ParseTreeNode, ParseTreeEdge>, SentenceInstance> buildPatternTrees(
			String patternTreeFile, String triggers[]) throws Exception{
		List<SentenceInstance> sentenceInsts = DocumentParser.loadSentences(patternTreeFile, triggers);
		ArrayList<Tree> parseTrees = DocumentParser.loadParseTrees(patternTreeFile);
		Map<edu.uci.ics.jung.graph.Tree<ParseTreeNode, ParseTreeEdge>, SentenceInstance> augmentTrees 
			= new HashMap<edu.uci.ics.jung.graph.Tree<ParseTreeNode, ParseTreeEdge>, SentenceInstance>();

		for (int i = 0; i < sentenceInsts.size(); i++) {
			Tree parse = parseTrees.get(i);
			SentenceInstance instance = sentenceInsts.get(i);
			edu.uci.ics.jung.graph.Tree<ParseTreeNode, ParseTreeEdge> augmentedTree = augmentParseTree(
					parse, instance);

			if(augmentedTree == null) continue;
			
			String sourceEntity = instance.getSourceEntity();
			String targetEntity = instance.getTargetEntity();
			String[] leafTexts = convert2Array(sourceEntity, targetEntity);
			augmentedTree = pruneTree(augmentedTree, leafTexts);
			augmentTrees.put(augmentedTree, instance);
		}

		return augmentTrees;
	}
	
	/**
	 * 
	 * @param str
	 * @return
	 */
	public String[] convert2Array(String... str) {
		List<String> list = new ArrayList<String>();
		for (String s : str) {
			String[] parts = s.split("\\s");
			for (String part : parts) {
				if (!list.contains(part)) {
					list.add(part);
				}
			}
		}

		return list.toArray(new String[0]);
	}

	/**
	 * Prune the tree by specifying the leaf nodes.
	 * 
	 * @param tree
	 * @param leafTexts
	 * @return
	 */
	private edu.uci.ics.jung.graph.Tree<ParseTreeNode, ParseTreeEdge> pruneTree(
			edu.uci.ics.jung.graph.Tree<ParseTreeNode, ParseTreeEdge> tree,
			String[] leafTexts) {

		edu.uci.ics.jung.graph.Tree<ParseTreeNode, ParseTreeEdge> prunedTree = tree;
		try {
			Collection<ParseTreeNode> nodes = tree.getVertices();
			int height = tree.getHeight();

			for (ParseTreeNode node : nodes) {
				if (node.isLeaf())
					continue;

				edu.uci.ics.jung.graph.Tree<ParseTreeNode, ParseTreeEdge> subtree = TreeUtils
						.getSubTree(tree, node);
				if (hasLeaves(subtree, leafTexts)) {
					int subTreeHeight = subtree.getHeight();
					if (subTreeHeight < height) {
						height = subTreeHeight;
						prunedTree = subtree;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return prunedTree;
	}

	/**
	 * Check if the tree has leaves.
	 * 
	 * @param tree
	 * @param leafTexts
	 * @return
	 */
	private boolean hasLeaves(
			edu.uci.ics.jung.graph.Tree<ParseTreeNode, ParseTreeEdge> tree,
			String[] leafTexts) {
		ArrayList<ParseTreeNode> leaves = getLeaves(tree);

		boolean hasLeafs = true;
		for (String txt : leafTexts) {

			boolean flag = false;
			for (ParseTreeNode leaf : leaves) {
				if (txt.equals(leaf.getNodeText())) {
					flag = true;
					break;
				}
			}

			if (!flag) {// the leaf is not found
				hasLeafs = false;
				break;
			}
		}

		return hasLeafs;
	}

	/**
	 * Count the number of nodes.
	 * 
	 * @param parse
	 * @param count
	 */
	private void countNodes(Tree parse) {
		Tree[] children = parse.children();
		if (children.length > 0) {
			mNodeCount += children.length;

			for (Tree child : children) {
				countNodes(child);
			}
		}
	}

	/**
	 * Collapse the entity nodes, augument other nodes with lemma, POS
	 * 
	 * @param parse
	 * @param instance
	 */
	public edu.uci.ics.jung.graph.Tree<ParseTreeNode, ParseTreeEdge> augmentParseTree(
			Tree parse, SentenceInstance instance) {
		mNodeCount = 1;
		countNodes(parse);

		Tree root = getRoot(parse);
		if(root == null) return null;
		
		Forest<ParseTreeNode, ParseTreeEdge> forest = new DelegateForest<ParseTreeNode, ParseTreeEdge>();
		ParseTreeNode parent = new ParseTreeNode(root.value(), mNodeCount);
		forest.addVertex(parent);
		growTree(root, parent, forest);

		edu.uci.ics.jung.graph.Tree<ParseTreeNode, ParseTreeEdge> agumentedTree = forest
				.getTrees().iterator().next();
		ArrayList<ParseTreeNode> leaves = getLeaves(agumentedTree);
		modifyLeafIndex(leaves);
		enrichLeaves(leaves, instance);

		return agumentedTree;
	}

	/**
	 * Get leaves of a tree.
	 * 
	 * @param tree
	 * @return
	 */
	public ArrayList<ParseTreeNode> getLeaves(
			edu.uci.ics.jung.graph.Tree<ParseTreeNode, ParseTreeEdge> tree) {
		ArrayList<ParseTreeNode> leaves = new ArrayList<ParseTreeNode>();
		Collection<ParseTreeNode> childs = tree.getVertices();
		for (ParseTreeNode node : childs) {
			if (node.isLeaf()) {
				leaves.add(node);
			}
		}

		return leaves;
	}

	/**
	 * Modify leaf node index and assign a new index starting from 1 to the
	 * number of tokens in the sentence.
	 * 
	 * @param tree
	 */
	private void modifyLeafIndex(ArrayList<ParseTreeNode> leaves) {
		Map<Integer, String> map = new HashMap<Integer, String>();
		for (ParseTreeNode node : leaves) {
			if (node.isLeaf()) {
				int nodeIndex = node.getNodeIndex();
				String nodeText = node.getNodeText();
				map.put(nodeIndex, nodeText);
			}
		}

		SortedSet<Integer> sortedIndices = new TreeSet<Integer>(map.keySet());
		int newIndex = 1;
		for (int nodeIndex : sortedIndices) {
			String nodeText = map.get(nodeIndex);

			for (ParseTreeNode node : leaves) {
				if ((node.getNodeIndex() == nodeIndex)
						&& (node.getNodeText().equalsIgnoreCase(nodeText))) {
					node.setNodeIndex(newIndex);
				}
			}

			newIndex++;
		}
	}

	/**
	 * Get the root node of the tree.
	 * 
	 * @param t
	 * @return
	 */
	private Tree getRoot(Tree t) {
		if(t.getChildrenAsList() == null || t.getChildrenAsList().isEmpty())
			return null;
		else
			return t.getChildrenAsList().get(0);
	}

	/**
	 * Grow a tree with an addition of indices
	 * 
	 * @param root
	 * @param parent
	 * @param forest
	 */
	private void growTree(Tree root, ParseTreeNode parent,
			Forest<ParseTreeNode, ParseTreeEdge> forest) {
		Tree[] children = root.children();
		if (children.length > 0) {
			for (Tree ch : children) {
				mNodeCount++;

				ParseTreeNode child = new ParseTreeNode(ch.value(), mNodeCount);
				if (ch.isLeaf())
					child.setAsLeaf(true);

				ParseTreeEdge edge = new ParseTreeEdge(edgeFactory.create(),
						parent, child);
				forest.addEdge(edge, parent, child);

				growTree(ch, child, forest);
			}
		}
	}

	/**
	 * Enrich leaf nodes with lemma and NER types.
	 * 
	 * @param leaves
	 * @param instance
	 */
	private void enrichLeaves(ArrayList<ParseTreeNode> leaves,
			SentenceInstance instance) {

		for (ParseTreeNode leaf : leaves) {
			String key = leaf.getNodeText() + "-" + leaf.getNodeIndex();
			
			String lemma = getTokenProperty(leaf.getNodeText(), leaf.getNodeIndex(), instance, "lemma");
			leaf.setNodeLemma(lemma);
			String pos = getTokenProperty(leaf.getNodeText(), leaf.getNodeIndex(), instance, "pos");
			leaf.setNodePOS(pos);
			String ner = getTokenProperty(leaf.getNodeText(), leaf.getNodeIndex(), instance, "ner");
			leaf.setNodeNER(ner);
		}
	}

	/**
	 * Get token property including, pos, lemma, ner
	 * @param text
	 * @param index
	 * @param instance
	 * @param type
	 * @return
	 */
	public String getTokenProperty(String text, int index, SentenceInstance instance, String type){
		List<TokenFeature> tokens = instance.getSentenceTokens();
		for(TokenFeature token : tokens){
			if(token.getFeatureText().equalsIgnoreCase(text) && (token.getPosition() == index )){
				if(type.equals("lemma"))
					return token.getLemma();
				if(type.equals("pos"))
					return token.getPOS();
				if(type.equals("ner"))
					return token.getNER();
			}
		}
		
		return null;
	}
	
	public static void main(String[] args) {
		String treeFile = "data/output4.xml";
		TreeBuilder tb = new TreeBuilder(treeFile);
		// tb.buildTrees();
		
	}

}
