package com.articulate.sigma;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.text.Utilities;
import javax.swing.tree.TreeNode;

import edu.uci.ics.jung.graph.DelegateForest;
import edu.uci.ics.jung.graph.Forest;
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
			double k_1_2 = continguousSubtreeKernel(tree1, tree2, root1, root2);
			if(k_1_2 <= 0.0)
				return 0.0;
			
			double k1 = continguousSubtreeKernel(tree1, tree1, root1, root1);
			double k2 = continguousSubtreeKernel(tree2, tree2, root2, root2);
			double normTerm = Math.sqrt(k1 * k2);
			
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
	/**
	 * Test case one: tree with different number of children
	 * Tree 1:
	 * 		a
	 *     /
	 *    /
	 *   b
	 *   
	 *   Tree 2:
	 * 		a
	 *     /\
	 *    /  \
	 *   b    c
	 *   
	 *   similarity between the two trees is 0, since they have different number of children.
	 */
	public static void testCaseOne(){
		Forest<ParseTreeNode, ParseTreeEdge> tree = new DelegateForest<ParseTreeNode, ParseTreeEdge>();
		ParseTreeNode node0 = new ParseTreeNode("a", 0);
		ParseTreeNode node1 = new ParseTreeNode("b", 1);
		
		ParseTreeEdge edge1 = new ParseTreeEdge(1, node0, node1);
		tree.addVertex(node0);
		tree.addEdge(edge1, node0, node1);
		
		Forest<ParseTreeNode, ParseTreeEdge> tree2 = new DelegateForest<ParseTreeNode, ParseTreeEdge>();
		
		ParseTreeNode node20 = new ParseTreeNode("a", 0);
		ParseTreeNode node21 = new ParseTreeNode("b", 1);
		ParseTreeNode node22 = new ParseTreeNode("c", 2);
		
		ParseTreeEdge edge21 = new ParseTreeEdge(1, node20, node21);
		ParseTreeEdge edge22 = new ParseTreeEdge(2, node20, node22);
		tree2.addVertex(node20);
		tree2.addEdge(edge21, node20, node21);
		tree2.addEdge(edge22, node20, node22);
		
		TreeKernel tk = new TreeKernel();
		
		Tree<ParseTreeNode, ParseTreeEdge> t1 = tree.getTrees().iterator().next();
		Tree<ParseTreeNode, ParseTreeEdge> t2 = tree2.getTrees().iterator().next();
		double unnormalize = tk.treeKernelSim(t1, t2);
		
		TreeKernel tk_norm = new TreeKernel(true);
		double normalize = tk_norm.treeKernelSim(t1, t2);
		System.out.println("unnormalized sim: " + unnormalize + "\tnormalized sim: " + normalize);
	}
	/**
	 * Test case two: tree with the same number of children, and the children are the same as well
	 * Tree 1:
	 * 	    a
	 *     /\
	 *    /  \
	 *   b    c
	 *   
	 *   Tree 2:
	 * 		a
	 *     /\
	 *    /  \
	 *   b    c
	 *   
	 *   similarity between the two trees is 2.5 (un-normalized), and 1 (normalized).
	 */
	public static void testCaseTwo(){
		Forest<ParseTreeNode, ParseTreeEdge> tree = new DelegateForest<ParseTreeNode, ParseTreeEdge>();
		ParseTreeNode node0 = new ParseTreeNode("a", 0);
		ParseTreeNode node1 = new ParseTreeNode("b", 1);
		ParseTreeNode node2 = new ParseTreeNode("c", 2);
		
		ParseTreeEdge edge1 = new ParseTreeEdge(1, node0, node1);
		ParseTreeEdge edge2 = new ParseTreeEdge(2, node0, node2);
		
		tree.addVertex(node0);
		tree.addEdge(edge1, node0, node1);
		tree.addEdge(edge2, node0, node2);
		
		Forest<ParseTreeNode, ParseTreeEdge> tree2 = new DelegateForest<ParseTreeNode, ParseTreeEdge>();
		
		ParseTreeNode node20 = new ParseTreeNode("a", 0);
		ParseTreeNode node21 = new ParseTreeNode("b", 1);
		ParseTreeNode node22 = new ParseTreeNode("c", 2);
		
		ParseTreeEdge edge21 = new ParseTreeEdge(1, node20, node21);
		ParseTreeEdge edge22 = new ParseTreeEdge(2, node20, node22);
		tree2.addVertex(node20);
		tree2.addEdge(edge21, node20, node21);
		tree2.addEdge(edge22, node20, node22);
		
		TreeKernel tk = new TreeKernel();
		
		Tree<ParseTreeNode, ParseTreeEdge> t1 = tree.getTrees().iterator().next();
		Tree<ParseTreeNode, ParseTreeEdge> t2 = tree2.getTrees().iterator().next();
		double unnormalize = tk.treeKernelSim(t1, t2);
		
		TreeKernel tk_norm = new TreeKernel(true);
		double normalize = tk_norm.treeKernelSim(t1, t2);
		System.out.println("unnormalized sim: " + unnormalize + "\tnormalized sim: " + normalize);
	}
	
	/**
	 * Test case three: root nodes have the same number of children, 
	 * but children in two trees have different number of grand children.
	 * 
	 * Tree 1:
	 * 	    a
	 *     /\
	 *    /  \
	 *   b    c
	 *       /
	 *      /
	 *     e
	 *   Tree 2:
	 * 		a
	 *     /\
	 *    /  \
	 *   b    c
	 *       /\
	 *      /  \
	 *     e    f
	 *   
	 *   similarity between the two trees is 1.75 (un-normalized), and 0.542 (normalized).
	 */
	public static void testCaseThree(){
		Forest<ParseTreeNode, ParseTreeEdge> tree = new DelegateForest<ParseTreeNode, ParseTreeEdge>();
		ParseTreeNode node0 = new ParseTreeNode("a", 0);
		ParseTreeNode node1 = new ParseTreeNode("b", 1);
		ParseTreeNode node2 = new ParseTreeNode("c", 2);
		ParseTreeNode node3 = new ParseTreeNode("e", 3);
		
		ParseTreeEdge edge1 = new ParseTreeEdge(1, node0, node1);
		ParseTreeEdge edge2 = new ParseTreeEdge(2, node0, node2);
		ParseTreeEdge edge3 = new ParseTreeEdge(3, node2, node3);
		
		tree.addVertex(node0);
		tree.addEdge(edge1, node0, node1);
		tree.addEdge(edge2, node0, node2);
		tree.addEdge(edge3, node2, node3);
		
		Forest<ParseTreeNode, ParseTreeEdge> tree2 = new DelegateForest<ParseTreeNode, ParseTreeEdge>();
		
		ParseTreeNode node20 = new ParseTreeNode("a", 0);
		ParseTreeNode node21 = new ParseTreeNode("b", 1);
		ParseTreeNode node22 = new ParseTreeNode("c", 2);
		ParseTreeNode node23 = new ParseTreeNode("e", 3);
		ParseTreeNode node24 = new ParseTreeNode("f", 4);
		
		ParseTreeEdge edge21 = new ParseTreeEdge(1, node20, node21);
		ParseTreeEdge edge22 = new ParseTreeEdge(2, node20, node22);
		ParseTreeEdge edge23 = new ParseTreeEdge(3, node22, node23);
		ParseTreeEdge edge24 = new ParseTreeEdge(4, node22, node24);
		tree2.addVertex(node20);
		tree2.addEdge(edge21, node20, node21);
		tree2.addEdge(edge22, node20, node22);
		tree2.addEdge(edge23, node22, node23);
		tree2.addEdge(edge24, node22, node24);
		
		TreeKernel tk = new TreeKernel();
		Tree<ParseTreeNode, ParseTreeEdge> t1 = tree.getTrees().iterator().next();
		Tree<ParseTreeNode, ParseTreeEdge> t2 = tree2.getTrees().iterator().next();
		double unnormalize = tk.treeKernelSim(t1, t2);
		
		TreeKernel tk_norm = new TreeKernel(true);
		double normalize = tk_norm.treeKernelSim(t1, t2);
		System.out.println("unnormalized sim: " + unnormalize + "\tnormalized sim: " + normalize);
	}
	/**
	 * Test case two: root have the same number of children, but one of their child is different.
	 * Also, the child (c) has the same number of grand children in both trees.
	 * 
	 * Tree 1:
	 * 	    a
	 *     /\
	 *    /  \
	 *   j    c
	 *       /\
	 *      /  \
	 *     e    f
	 *   Tree 2:
	 * 		a
	 *     /\
	 *    /  \
	 *   b    c
	 *       /\
	 *      /  \
	 *     e    f
	 *   
	 *   similarity between the two trees is 2.875 (un-normalized), and 0.79 (normalized).
	 */
	public static void testCaseFour(){
		Forest<ParseTreeNode, ParseTreeEdge> tree = new DelegateForest<ParseTreeNode, ParseTreeEdge>();
		ParseTreeNode node0 = new ParseTreeNode("a", 0);
		ParseTreeNode node1 = new ParseTreeNode("j", 1);
		ParseTreeNode node2 = new ParseTreeNode("c", 2);
		ParseTreeNode node3 = new ParseTreeNode("e", 3);
		ParseTreeNode node4 = new ParseTreeNode("f", 4);
		
		ParseTreeEdge edge1 = new ParseTreeEdge(1, node0, node1);
		ParseTreeEdge edge2 = new ParseTreeEdge(2, node0, node2);
		ParseTreeEdge edge3 = new ParseTreeEdge(3, node2, node3);
		ParseTreeEdge edge4 = new ParseTreeEdge(4, node2, node4);
		
		tree.addVertex(node0);
		tree.addEdge(edge1, node0, node1);
		tree.addEdge(edge2, node0, node2);
		tree.addEdge(edge3, node2, node3);
		tree.addEdge(edge4, node2, node4);
		
		Forest<ParseTreeNode, ParseTreeEdge> tree2 = new DelegateForest<ParseTreeNode, ParseTreeEdge>();
		
		ParseTreeNode node20 = new ParseTreeNode("a", 0);
		ParseTreeNode node21 = new ParseTreeNode("b", 1);
		ParseTreeNode node22 = new ParseTreeNode("c", 2);
		ParseTreeNode node23 = new ParseTreeNode("e", 3);
		ParseTreeNode node24 = new ParseTreeNode("f", 4);
		
		ParseTreeEdge edge21 = new ParseTreeEdge(1, node20, node21);
		ParseTreeEdge edge22 = new ParseTreeEdge(2, node20, node22);
		ParseTreeEdge edge23 = new ParseTreeEdge(3, node22, node23);
		ParseTreeEdge edge24 = new ParseTreeEdge(4, node22, node24);
		tree2.addVertex(node20);
		tree2.addEdge(edge21, node20, node21);
		tree2.addEdge(edge22, node20, node22);
		tree2.addEdge(edge23, node22, node23);
		tree2.addEdge(edge24, node22, node24);
		
		TreeKernel tk = new TreeKernel();
		
		Tree<ParseTreeNode, ParseTreeEdge> t1 = tree.getTrees().iterator().next();
		Tree<ParseTreeNode, ParseTreeEdge> t2 = tree2.getTrees().iterator().next();
		double unnormalize = tk.treeKernelSim(t1, t2);
		
		TreeKernel tk_norm = new TreeKernel(true);
		double normalize = tk_norm.treeKernelSim(t1, t2);
		System.out.println("unnormalized sim: " + unnormalize + "\tnormalized sim: " + normalize);
	}
	
	public static void main(String[] args){
		testCaseOne();
		testCaseTwo();
		testCaseThree();
		testCaseFour();
	}
}