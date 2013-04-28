package com.articulate.sigma;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.org.apache.xerces.internal.parsers.XMLDocumentParser;

import edu.stanford.nlp.trees.MemoryTreebank;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeNormalizer;

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
	
	/**
	 * Get name mentions from a feature list
	 * @param features the feature list
	 * @param type the type of a mention, location, organization, person
	 * @return
	 */
	public static List<TokenFeature> getNamedMentions(List<TokenFeature> features, String type){
		
		List<TokenFeature> mentions = new ArrayList<TokenFeature>();
		
		for(int i=0; i<features.size() -1; i++){
			
			String mention = "";
			TokenFeature featPrev = features.get(i);
			
			if(featPrev.getNER().equalsIgnoreCase(type)){
				mention += featPrev.getFeatureText() + " ";
				for(int j = i + 1; j < features.size(); j++){
					
					if( j == (features.size() - 1) )
						i = j;
					
					TokenFeature featNext = features.get(j);
					if(featNext.getNER().equalsIgnoreCase(type)){
						mention += featNext.getFeatureText() + " ";
					}else{
						i = j;
						break;
					}
				}
			}
			
			mention = mention.trim();
			if(mention == null || mention.isEmpty()) continue;
			if(!isValidMention(mention)) continue;
			
			int position = featPrev.getPosition();
			TokenFeature name = new TokenFeature(mention, position);
			if(!mentions.contains(name))
				mentions.add(name);
		}
		
		return mentions;
		
	}
	
	public static boolean isValidMention(String mention){
		if(mention.split("\\s").length > 3) return false;
		if(isAllPunc(mention)) return false;
		if(isMessyWord(mention)) return false;
		
		return true;
	}
	
	/**
	 * Check if the string contains all punctuations.
	 * @param str
	 * @return
	 */
	public static boolean isAllPunc(String str) {
		str = str.replace(" ", "").replace("\t", "");
		Pattern pattern = Pattern.compile("\\p{Punct}");
		Matcher match = null;
		for (int i = 0; i < str.length(); i++) {
			char ch = str.charAt(i);
			match = pattern.matcher(Character.toString(ch));
			if (!match.find())
				return false;
		}

		return true;
	}
	
	/**
	 * Check the word is messy word or not
	 * @param word
	 */
	public static boolean isMessyWord(String word){
		for (int i = 0; i < word.length(); i++) {
			char ch = word.charAt(i);
			if (!isPuncLetterSpace(ch)) {
				return true;
			}
		}
		return false;
	}
	/**
	 * Check if the char is puncuation, letter or space or not.
	 * @param ch
	 * @return
	 */
	public static boolean isPuncLetterSpace(char ch) {
		Pattern pattern = Pattern.compile("[\\p{Alnum}\\p{Punct}\\p{Space}]");
		Matcher match = pattern.matcher(Character.toString(ch));
		if (match.find())
			return true;
		return false;
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
	
	/**
	 * Get sentence attribute value, for example, sentence id, sentence arguments, sentence label
	 * @param sentenceNode
	 * @param att
	 * @return
	 */
	protected static String getSentenceAttributeValue(Node sentenceNode, String att){
		if(sentenceNode.getAttributes().getNamedItem(att) != null){
			return sentenceNode.getAttributes().getNamedItem(att).getNodeValue();
		}else
			return null;
	}
	
	/**
	 * Load sentence instance in an xml file.
	 * @param xmlFile
	 * @return
	 * @throws Exception
	 */
	public static List<SentenceInstance> loadSentences(String xmlFile, String triggers[]) throws Exception{
		
		List<SentenceInstance> instances = new ArrayList<SentenceInstance>();
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = dbf.newDocumentBuilder();
		NodeList sentenceNodeList = docBuilder.parse(xmlFile).getElementsByTagName("sentences");
		List<Node> sentenceNodes = new ArrayList<Node>();
		extractElementNodes(sentenceNodeList, "sentence", sentenceNodes);
		
		int id = 1;
		for(Node sentenceNode : sentenceNodes){
			
			String sourceEntity = getSentenceAttributeValue(sentenceNode, "sourceEntity");
			String targetEntity = getSentenceAttributeValue(sentenceNode, "targetEntity");
			
			
			List<TokenFeature> tokenFeatures = new ArrayList<TokenFeature>();
			NodeList nodeList = sentenceNode.getChildNodes();
			
			boolean hasTrigger = false;
			
			for(int i=0; i<nodeList.getLength(); i++){
				Node tokensNodes = nodeList.item(i);
				if(!tokensNodes.getNodeName().equalsIgnoreCase("tokens")) continue;
				
				NodeList tokensSubChildList = tokensNodes.getChildNodes();
				List<Node> tokenNodes = new ArrayList<Node>();
				extractElementNodes(tokensSubChildList, "token", tokenNodes);
				
				String word = "", lemma = "", pos = "", ner= "", index = "";
				for(Node tokenNode : tokenNodes){
					
					index = tokenNode.getAttributes().getNamedItem("id").getNodeValue();
					
					NodeList subChildList = tokenNode.getChildNodes();
					for(int j=0; j<subChildList.getLength(); j++){
						Node childNode = subChildList.item(j);
						if(childNode.getNodeName().equalsIgnoreCase("word")){
							word = getText(childNode);
						}
						
						if(childNode.getNodeName().equalsIgnoreCase("lemma")){
							lemma = getText(childNode);
						}
						
						if(childNode.getNodeName().equalsIgnoreCase("POS")){
							pos = getText(childNode);
						}
						
						if(childNode.getNodeName().equalsIgnoreCase("NER")){
							ner = getText(childNode);
						}
					}
					
					for(String trigger : triggers){
						if(word.equalsIgnoreCase(trigger)){
							hasTrigger = true;
							break;
						}
					}
					
					TokenFeature feat = new TokenFeature(word, Integer.parseInt(index));
					feat.setLemma(lemma);
					feat.setPOS(pos);
					feat.setNER(ner);
					
					tokenFeatures.add(feat);
				}
			}
			
			if(hasTrigger){
				SentenceInstance instance = new SentenceInstance(tokenFeatures);
				instance.setSentenceId(id);
				instance.setSourceEntity(sourceEntity);
				instance.setTargetEntity(targetEntity);
				instances.add(instance);
			}
			
			id ++;
		}
		
		return instances;
	}
	/**
	 * Load parse trees from the xml file.
	 * @return
	 */
	public static ArrayList<Tree> loadParseTrees(String xmlFile){
		ArrayList<Tree> parseTrees = new ArrayList<Tree>();
		try{
			MemoryTreebank treeBank = new MemoryTreebank(new TreeNormalizer());
			treeBank.loadPath(xmlFile);
			Iterator<Tree> tbIterator = treeBank.iterator();
			while(tbIterator.hasNext()){
				Tree t = tbIterator.next();
			    parseTrees.add(t);			    
			}
			
		}catch(Exception e){e.printStackTrace();}
		
		return parseTrees;
	}
}
