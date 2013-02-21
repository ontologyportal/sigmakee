package com.articulate.sigma;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.stanford.nlp.trees.MemoryTreebank;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeNormalizer;

public class SentenceParser extends DocumentParser{
	public String mXmlFile;
	public SentenceParser(String xmlFile){
		mXmlFile = xmlFile;
	}
	/**
	 * Load parse trees from the xml file.
	 * @return
	 */
	public ArrayList<Tree> loadParseTrees(){
		ArrayList<Tree> parseTrees = new ArrayList<Tree>();
		try{
			MemoryTreebank treeBank = new MemoryTreebank(new TreeNormalizer());
			treeBank.loadPath(mXmlFile);
			Iterator<Tree> tbIterator = treeBank.iterator();
			while(tbIterator.hasNext()){
				Tree t = tbIterator.next();
			    parseTrees.add(t);			    
			}
			
		}catch(Exception e){e.printStackTrace();}
		
		return parseTrees;
	}
	/**
	 * Load the sentence instances, including
	 * sentence label, entities, tokens, token lemma, token pos, 
	 * ner type
	 * @param xmlFile
	 * @return
	 */
	public ArrayList<SentenceInstance> loadSentenceInstances(){
		ArrayList<SentenceInstance> instances = new ArrayList<SentenceInstance>();
		try{
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = dbf.newDocumentBuilder();
			Document doc = docBuilder.parse(mXmlFile);
			NodeList nodeList = doc.getElementsByTagName("sentences");
			
			List<Node> sentenceNodes = new ArrayList<Node>();
			
			extractElementNodes(nodeList, "sentence", sentenceNodes);
			for(Node sentenceNode : sentenceNodes){
				SentenceInstance instance = new SentenceInstance();
				//Parse the token properties, including lemma, part of speech, ner type
				parseTokenProperties(instance, sentenceNode.getChildNodes());
				
				instances.add(instance);
			}
		}catch(Exception e){e.printStackTrace();}		
		
		return instances;
	}
	
	/**
	 * Parse the token properties, including lemma, part of speech, ner type
	 * @param instance
	 * @param nodeList
	 */
	protected static void parseTokenProperties(SentenceInstance instance, NodeList nodeList){
		for(int i=0; i<nodeList.getLength(); i++){
			Node tokensNodes = nodeList.item(i);
			if(!tokensNodes.getNodeName().equalsIgnoreCase("tokens")) continue;
			
			Map<String, String> words2Lemma = new HashMap<String, String>();
			Map<String, String> words2POS = new HashMap<String, String>();
			Map<Integer, String> index2Word =  new HashMap<Integer, String>();
			Map<String, String> words2NER = new HashMap<String, String>();
			
			NodeList tokensSubChildList = tokensNodes.getChildNodes();
			List<Node> nodes = new ArrayList<Node>();
			extractElementNodes(tokensSubChildList, "token", nodes);
			
			for(Node node : nodes){
				String tokenId = node.getAttributes().getNamedItem("id").getNodeValue();
				NodeList subChildList = node.getChildNodes();
				
				String word = "", lemma = "", POS = "", NER = "";
				for(int j=0; j<subChildList.getLength(); j++){
					Node childNode = subChildList.item(j);
					if(childNode.getNodeName().equalsIgnoreCase("word"))
						word = getText(childNode);
					if(childNode.getNodeName().equalsIgnoreCase("lemma"))
						lemma = getText(childNode);
					if(childNode.getNodeName().equalsIgnoreCase("POS"))
						POS = getText(childNode);
					if(childNode.getNodeName().equalsIgnoreCase("NER"))
						NER = getText(childNode);
				}
				
				if(word.isEmpty() || lemma.isEmpty() || POS.isEmpty()) continue;
				word += "-" + tokenId;
				
				words2Lemma.put(word, lemma);
				words2POS.put(word, POS);
				words2NER.put(word, NER);
			}
			
			instance.setWords2Lemma(words2Lemma);
			instance.setWords2POS(words2POS);
			instance.setWords2NER(words2NER);
		}
	}
	
	/**
	 * Get text of a node.
	 * @param node
	 * @return
	 */
	public static String getText(Node node){
		NodeList childNodes = node.getChildNodes();
		for(int x=0; x<childNodes.getLength(); x++){
			Node childNode = childNodes.item(x);
			return childNode.getNodeValue();
		}
		return null;
	}
}
