package com.articulate.sigma;

import java.io.File;
import java.io.PrintWriter;
import java.util.List;
import java.util.Properties;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokenEndAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
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
	    props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse");
		pipeline = new StanfordCoreNLP(props);
	}
	
	/**
	 * Get the header of the annotated file
	 * @return
	 */
	public String getAnnotationHeader(){
		String header = "";
		header += "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
		header += "<?xml-stylesheet href=\"CoreNLP-to-HTML.xsl\" type=\"text/xsl\"?>\n";
		header += "<root>\n";
		header += "  <document>\n";
		header += "    <sentences>\n";
		
		return header;
	}
	/**
	 * Get the tail of the annoted
	 * @return
	 */
	public String getAnnotationTail(){
		String tail = "";
		tail += "    </sentences>\n";
		tail += "  </document>\n";
		tail += "</root>\n";
		
		return tail;
	}
	
	/**
	 * Get annotated tokens.
	 * @param sentence
	 * @return
	 */
	public String getAnnotedTokens(CoreMap sentence){
		
		String annotatedTokens = "";
		annotatedTokens += "        <tokens>\n";
		for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
			
			Integer index = token.get(TokenEndAnnotation.class);
			String word = token.get(TextAnnotation.class);
			String pos = token.get(PartOfSpeechAnnotation.class);
			String lemma = token.get(LemmaAnnotation.class);
			String ne = token.get(NamedEntityTagAnnotation.class); 
			
			annotatedTokens +="          <token id=\"" + index + "\">\n";
			annotatedTokens +="            <word>" + word + "</word>\n";
			annotatedTokens +="            <lemma>" + lemma + "</lemma>\n";
			annotatedTokens += "            <POS>" + pos + "</POS>\n";
			annotatedTokens += "            <NER>" + ne + "</NER>\n";
			annotatedTokens += "          </token>\n";
		}
		
		annotatedTokens += "        </tokens>\n";
		
		return annotatedTokens;
	}
	/**
	 * Annotate the text
	 * @param text the plain Wikipedia text
	 * @param triggerWord the trigger word used for selecting sentences
	 * @return annotated text
	 */
	public String annotateText(String text, String triggerWord){
		Annotation annotation = new Annotation(text);
		pipeline.annotate(annotation);
		List<CoreMap> sentences = annotation.get(SentencesAnnotation.class);
		
		String annotatedText = "";
		for( CoreMap sentence : sentences ){
			if( !hasTriggerWord(sentence, triggerWord) ) continue;
			
			annotatedText += "      <sentence>\n";
			
			String annotatedTokens = getAnnotedTokens(sentence);
			annotatedText += annotatedTokens;
			
			Tree tree = sentence.get(TreeAnnotation.class);
			String tree_string = tree.toString();
			annotatedText += "        <parse>" + tree_string + "</parse>\n";
			
			annotatedText += "      </sentence>\n";
		}
		
		return annotatedText;
	}
	/**
	 * Check if the sentence contains the trigger word.
	 * @param sentence
	 * @param triggerWord trigger word used for filtering sentences
	 * @return
	 */
	public boolean hasTriggerWord(CoreMap sentence, String triggerWord){
		for (CoreLabel token : sentence.get(TokensAnnotation.class)) {	
			
			String word = token.get(TextAnnotation.class);
			String lemma = token.get(LemmaAnnotation.class);
			if(word.equalsIgnoreCase(triggerWord) || lemma.equalsIgnoreCase(triggerWord))
				return true;
		}
		return false;
	}
	
	/**
	 * Batch annotate texts.
	 * @param corpusDir the directory of a corpus
	 * @param annoFile  the result annotation file (in XML format)
	 * @param triggerWord  the trigger word for selecting sentences
	 */
	public void batchAnnotate(String corpusDir, String annoFile, String triggerWord){
		try{
			PrintWriter writer = FileUtils.getWriter(annoFile);
			String header = getAnnotationHeader();
			writer.write(header);
			
			File[] fileList = new File(corpusDir).listFiles();
			for( File file : fileList ){
				String text = FileUtils.getTextFromFile(file);
				if(text == null || text.isEmpty()) continue;
				String annotatedText = annotateText(text, triggerWord);
				
				if( annotatedText == null || annotatedText.isEmpty() ) continue;
				writer.write(annotatedText);
				writer.flush();
			}
			
			String tail = getAnnotationTail();
			writer.write(tail);
			writer.flush();
			writer.close();
		}catch(Exception e){}
	}
}
