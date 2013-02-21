package com.articulate.sigma;

public class ParseTreeEdge {
	/**edge id*/
	public int mEdgeId;
	public ParseTreeNode mParent;
	public ParseTreeNode mChild;
	
	public ParseTreeEdge(int edgeId, ParseTreeNode parent, ParseTreeNode child){
		mEdgeId = edgeId;
		mParent = parent;
		mChild = child;
	}
	/**
	 * Set edge id 
	 * @param edgeId
	 */
	public void setEdgeId(int edgeId){
		mEdgeId = edgeId;
	}
	/**
	 * Get edge id
	 * @return
	 */
	public int getEdgeId(){
		return mEdgeId;
	}
	/**
	 * Set the parent node of an edge.
	 * @param parent
	 */
	public void setParent(ParseTreeNode parent){
		mParent = parent;
	}
	/**
	 * Get the parent node of an edge.
	 * @return
	 */
	public ParseTreeNode getParent(){
		return mParent;
	}
	/**
	 * Set the child node of an edge.
	 * @param child
	 */
	public void setChild(ParseTreeNode child){
		mChild = child;
	}
	/**
	 * get the child node of an edge.
	 * @return
	 */
	public ParseTreeNode getChild(){
		return mChild;
	}
}
