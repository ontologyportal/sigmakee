package com.articulate.sigma;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.text.Utilities;
import javax.swing.tree.TreeNode;

import edu.uci.ics.jung.graph.Tree;

/**
 * This class extracts computes similarity between trees using kernel methods.
 * The tree kernel algorithm is described in Dmitry Zelenko's Kernel Methods for
 * Relation Extraction (Zelenko et al, 2003).
 * 
 */
public class TreeKernel {
	public static double lambda = 0.5;
	
	/** the precalculated powers of lambda */
	protected double[] m_powersOflambda = null;
	/**
	 * powers of lambda are prepared prior to kernel evaluations. all powers
	 * between 0 and this value are precalculated
	 */
	protected static final int MAX_POWER_OF_LAMBDA = 10;
	/** flag to control whether normalize the tree similarity */
	private boolean mNormalize = false;

	public TreeKernel() {
		m_powersOflambda = precomputePowersOfLambda();
	}

	public TreeKernel(boolean normalize) {
		m_powersOflambda = precomputePowersOfLambda();
		mNormalize = normalize;
	}
	
	/**
	 * precalculates small powers of lambda to speed up the kernel evaluation
	 * 
	 * @return the powers
	 */
	private double[] precomputePowersOfLambda() {
		double[] powers = new double[MAX_POWER_OF_LAMBDA + 1];
		powers[0] = 1.0;
		double val = 1.0;
		for (int i = 1; i <= MAX_POWER_OF_LAMBDA; i++) {
			val *= lambda;
			powers[i] = val;
		}
		return powers;
	}

	/**
	 * Compute the similarity between two trees.
	 * 
	 * @param tree1
	 * @param tree2
	 * @return
	 */
	public double treeKernelSim(Tree<ParseTreeNode, ParseTreeEdge> tree1,
			Tree<ParseTreeNode, ParseTreeEdge> tree2) {

		ParseTreeNode root1 = tree1.getRoot();
		ParseTreeNode root2 = tree2.getRoot();
		
		if(mNormalize){
			double k1 = continguousSubtreeKernel(tree1, tree1, root1, root1);
			double k2 = continguousSubtreeKernel(tree2, tree2, root2, root2);
			double normTerm = Math.sqrt(k1 * k2);
			double k_1_2 = continguousSubtreeKernel(tree1, tree2, root1, root2);
			
			return k_1_2/normTerm;
			
		}else{
			return continguousSubtreeKernel(tree1, tree2, root1, root2);
		}
	}

	/**
	 * Continguous subtree kernel.
	 * 
	 * @param tree1
	 * @param tree2
	 * @param n1
	 * @param n2
	 * @return
	 */
	private double continguousSubtreeKernel(
			Tree<ParseTreeNode, ParseTreeEdge> tree1,
			Tree<ParseTreeNode, ParseTreeEdge> tree2, ParseTreeNode n1,
			ParseTreeNode n2) {

		double retVal = 0.0;

		List<ParseTreeNode> children1 = collection2List(tree1.getChildren(n1));
		List<ParseTreeNode> children2 = collection2List(tree2.getChildren(n2));
		int children1Cnt = children1.size();
		int children2Cnt = children2.size();

		if (children1Cnt != children2Cnt) {
			retVal = 0.0;
		} else if (children1Cnt == 1 && children2Cnt == 1
				&& tree1.getChildCount(children1.get(0)) == 0
				&& tree2.getChildCount(children2.get(0)) == 0
				&& areNodesEqual(children1.get(0), children2.get(0))) {
			// have same parents and both are preterminals
			retVal = 1.0 + lambda;
		} else if (areNodesEqual(n1, n2) && children1Cnt == 0
				&& children2Cnt == 0) {
			retVal = 1.0;
		} else {
			// At this point they have the same label and same # children.
			// Check if children the same.
			if (children1Cnt > 0) {// to test that it is a root node, and they
									// are equal
				if (areNodesEqual(n1, n2))
					retVal += 1.0;// compute the root similarity
			}

			for (int i = 0; i < children1Cnt; i++) {
				ParseTreeNode child1 = children1.get(i);
				ParseTreeNode child2 = children2.get(i);

				for (int j = 1; j <= children1Cnt; j++) {
					retVal += m_powersOflambda[j]
							* continguousSubtreeKernel(tree1, tree2, child1,
									child2);
				}
			}
		}

		return retVal;
	}

	/**
	 * Check if two nodes are equal by means of their NER type,
	 * text and lemma.
	 * 
	 * @param node1
	 * @param node2
	 * @return
	 */
	private boolean areNodesEqual(ParseTreeNode node1, ParseTreeNode node2) {

		//check if the two nodes are of the same named entity type
		if(node1.getNodeNER() != null && node2.getNodeNER() != null)
			if(node1.getNodeNER().equals(node2.getNodeNER()))
				return true;
		
		//check if the two node texts are equal
		if(node1.getNodeText() != null && node2.getNodeText() != null)
			if (node1.getNodeText().equalsIgnoreCase(node2.getNodeText()))
				return true;

		//check if the two node lemma are equal
		if(node1.getNodeLemma() != null && node2.getNodeLemma() != null)
			if (node1.getNodeLemma().equals(node2.getNodeLemma()))
				return true;

		return false;

	}

	/**
	 * Change the node list to the list of node texts.
	 * 
	 * @param nodes
	 * @return
	 */
	public List<String> toStringList(List<ParseTreeNode> nodes) {
		List<String> list = new ArrayList<String>();
		for (ParseTreeNode node : nodes) {
			list.add(node.getNodeText());
		}
		return list;
	}

	/**
	 * Collection to list conversion and arrange the node by order of their
	 * appearance in a sentence.
	 * 
	 * @param col
	 * @return
	 */
	private static List<ParseTreeNode> collection2List(
			Collection<ParseTreeNode> col) {
		Map<Integer, ParseTreeNode> map = new HashMap<Integer, ParseTreeNode>();
		Iterator<ParseTreeNode> iter = col.iterator();
		while (iter.hasNext()) {
			ParseTreeNode node = iter.next();
			int nodeIndx = node.getNodeIndex();
			map.put(nodeIndx, node);
		}

		List<ParseTreeNode> list = new ArrayList<ParseTreeNode>();
		for (Integer indx : map.keySet()) {
			ParseTreeNode item = map.get(indx);
			list.add(item);
		}

		return list;
	}
}