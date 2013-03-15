package com.articulate.sigma;

import java.io.File;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefChain.CorefMention;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations.CorefChainAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokenEndAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.trees.semgraph.SemanticGraph;
import edu.stanford.nlp.trees.semgraph.SemanticGraphCoreAnnotations.BasicDependenciesAnnotation;
import edu.stanford.nlp.trees.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.trees.semgraph.SemanticGraphCoreAnnotations.CollapsedDependenciesAnnotation;
import edu.stanford.nlp.trees.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.util.CoreMap;

/**
 * This class annotates text using Stanford CoreNLP package.
 * It includes tokenization, POS tagging, syntactic parsing, named entity recognition. 
 * All the annotated results will be written out to an xml file. 
 *
 */
public class TextAnnotator {
	public StanfordCoreNLP pipeline = null;
	
	public TextAnnotator(){
		Properties props= new Properties();
	    props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
		pipeline = new StanfordCoreNLP(props);
	}
	
	/**
	 * Get texts contains triggers from a given corpus.
	 * @param corpusDir the directory of a corpus
	 * @param outDir the output directory
	 * @param trigger the trigger word
	 * @throws Exception
	 */
	public static void getTextContainsTriggers(String corpusDir, String outDir, String trigger) throws Exception{
		PrintWriter writer = null;
		File[] fileList = new File(corpusDir).listFiles();
		for( File file : fileList ){
			String personName = removeExtension(file.getName());
			if(isMessyWord(personName)) continue;
			if(personName.contains("(")) continue;
			if(personName.split("\\s").length > 3) continue;
			if(personName.contains(" of ")) continue;
			
			String text = FileUtils.getTextFromFile(file);
			if(text == null || text.isEmpty()) continue;
			WikiArticleParser wap = new WikiArticleParser(text);
			text = wap.getPlainText().trim();
			
			if(!isTermInSequence(text, trigger)) continue;
			
			String outfile = outDir + "/" + personName + ".txt";
			writer = FileUtils.getWriter(outfile);
			writer.write(text + "\n");
			writer.flush();
		}
		
		writer.close();
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
	 * Annotate texts in a corpus, including tokenization, part-of-speech tagging,
	 * lemmatization, named entity recognition, co-reference resolution
	 * @param corpusDir the corpus directory
	 * @param outDir the output directory
	 */
	public void annotateTexts(String corpusDir, String outDir){
		try{
			PrintWriter writer = null;
			File[] fileList = new File(corpusDir).listFiles();
			for( File file : fileList ){
				String text = FileUtils.getTextFromFile(file);
				if(text == null || text.isEmpty()) continue;
				
				String personName = removeExtension(file.getName());
				String outfile = outDir + "/" + personName + ".xml";
				writer = new PrintWriter(outfile);
				
				Annotation annotation = new Annotation(text);
				pipeline.annotate(annotation);
				pipeline.xmlPrint(annotation, writer);
			}
			
			writer.close();
		}catch(Exception e){}
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
	 * Remove file name extension
	 * @param filename
	 * @return
	 */
	public static String removeExtension(String filename){
		int index = filename.lastIndexOf(".");
	    if (index == -1)
	        return filename;
	    
	    return filename.substring(0, index);
	}
	
	public static void main(String[] args) throws Exception{
		String corpusDir = args[0]; 
		String outDir = args[1];
		TextAnnotator ta = new TextAnnotator();
		ta.annotateTexts(corpusDir, outDir);
	}
}