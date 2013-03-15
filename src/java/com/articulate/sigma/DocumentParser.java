package com.articulate.sigma;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class DocumentParser {

	public static String m_nText = "";
	
	/**
	 * Get text from an xml element
	 * @param nodeList
	 * @param elementName
	 * @return
	 */
	public static String getElementText(NodeList nodeList, String elementName){
		getElement(nodeList, elementName);
		String str = new String(m_nText);
		m_nText = "";//this must be set to empty since it will be used by the next element.
		
		return str;
	}
	/**
	 * Recurse the node list to find the corresponding element text.
	 * @param nodeList
	 * @param elementName
	 */
	protected static void getElement(NodeList nodeList, String elementName){
		for(int i=0; i<nodeList.getLength(); i++){
			Node docNode = nodeList.item(i);
			if(docNode.getNodeName().equalsIgnoreCase(elementName)){
				getTextFromNode(docNode);
			}else{
				NodeList bodyChildNodes = docNode.getChildNodes();
				getElement(bodyChildNodes, elementName);
			}
		}
	}
	/**
	 * Set element nodes.
	 * @param nodeList
	 * @param elementName
	 * @param nodes
	 */
	protected static void extractElementNodes(NodeList nodeList, String elementName, List<Node> nodes){
		for(int i=0; i<nodeList.getLength(); i++){
			Node docNode = nodeList.item(i);
			if(docNode.getNodeName().equalsIgnoreCase(elementName)){
				nodes.add(docNode);
			}else{
				NodeList bodyChildNodes = docNode.getChildNodes();
				extractElementNodes(bodyChildNodes, elementName, nodes);
			}
		}
	}
	/**
	 * Get element id.
	 * @param nodeList
	 * @param elementName
	 * @param nameId
	 * @return
	 */
	public static String getElementID(NodeList nodeList, String elementName, String nameId){
		List<Node> nodes = new ArrayList<Node>();
		extractElementNodes(nodeList, elementName, nodes);
		if(nodes.size() <= 0) return null;
		String docId = nodes.get(0).getAttributes().getNamedItem(nameId).getNodeValue();
		return docId;
	}
	
	/**
	 * Get text from the text node.
	 * @param textNode
	 * @return
	 */
	protected static void getTextFromNode(Node textNode){
		NodeList childNodes = textNode.getChildNodes();
		
		for(int x=0; x<childNodes.getLength(); x++){
			Node childNode = childNodes.item(x);
			
			if(childNode.getChildNodes().getLength() > 0){
				getTextFromNode(childNode);
			}else{
				m_nText += childNode.getNodeValue().replace("\r\n", " ").replace("\n", " ").trim();
			}
		}	
	}
}
