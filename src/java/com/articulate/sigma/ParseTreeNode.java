package com.articulate.sigma;

import java.util.ArrayList;
import java.util.List;

public class ParseTreeNode {
	/** the node text */
	private String mNodeText = null;
	/**the node index */
	private int mNodeIndex;
	/**part of speech of the node */
	private String mNodePOS = null;
	/** named entity tag of the node, it might be organization, location and person */
	private String mNodeNER = null;
	private String mNodeLemma = null;
	private boolean mIsLeaf = false;
	
	
	private volatile int hashCode = 0;
	
	public ParseTreeNode(String nodeText, int nodeIndex){
		mNodeText = nodeText;
		mNodeIndex = nodeIndex;
	}
	
	/**
	 * Set node text.
	 * @param nodeText
	 */
	public void setNodeText(String nodeText){
		mNodeText = nodeText;
	}
	/**
	 * Get node text.
	 * @return
	 */
	public String getNodeText(){
		return mNodeText;
	}
	/**
	 * Set node index.
	 * @param nodeIndex
	 */
	public void setNodeIndex(int nodeIndex){
		mNodeIndex = nodeIndex;
	}
	/**
	 * Get node index.
	 * @return
	 */
	public int getNodeIndex(){
		return mNodeIndex;
	}
	
	/**
	 * Set the part of speech of the node
	 * @param pos
	 */
	public void setNodePOS(String pos){
		mNodePOS = pos;
	}
	/**
	 * Get part of speech of the node
	 * @return
	 */
	public String getNodePOS(){
		return mNodePOS;
	}
	/**
	 * Set the ner tag of a node
	 * @param ner
	 */
	public void setNodeNER(String ner){
		mNodeNER = ner;
	}
	/**
	 * Get the ner tag of a node.
	 * @return
	 */
	public String getNodeNER(){
		return mNodeNER;
	}
	/**
	 * Set the lemma of a node
	 * @param lemma
	 */
	public void setNodeLemma(String lemma){
		mNodeLemma = lemma;
	}
	/**
	 * Get the node lemma
	 * @return
	 */
	public String getNodeLemma(){
		return mNodeLemma;
	}
	
	/**
	 * Set the node as leaf.
	 * @param isLeaf
	 */
	public void setAsLeaf(boolean isLeaf){
		mIsLeaf = isLeaf;
	}
	public boolean isLeaf(){
		return mIsLeaf;
	}
	
	public String toString(){
		return mNodeText + "-" + mNodeIndex;
	}
	
	public boolean equals(Object obj){
		if(this == obj)
			return true;
		if(!(obj instanceof ParseTreeNode))
			return false;
		ParseTreeNode node = (ParseTreeNode) obj;
		return mNodeIndex == node.getNodeIndex() && mNodeText.equals(node.getNodeText()); 
	}
	
	public int hashCode(){
		final int multiplier = 23;
		if(hashCode == 0){
			int code = 133;
			code = multiplier * code + mNodeIndex;
			code = multiplier * code + mNodeText.hashCode();
			hashCode = code;
		}
		return hashCode;
	}
	
	public static void main(String[] args){
		
	}
}
