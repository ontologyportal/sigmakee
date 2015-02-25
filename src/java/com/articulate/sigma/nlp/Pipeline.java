package com.articulate.sigma.nlp;

import edu.stanford.nlp.pipeline.*;

import java.util.*;

import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.*;
import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations.CorefChainAnnotation;
import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.ling.CoreAnnotations.*;

import edu.stanford.nlp.parser.lexparser.*;

public class Pipeline {

    /** ***************************************************************
     */
    public static void main(String[] args) {

        // creates a StanfordCoreNLP object, with POS tagging, lemmatization, NER, parsing, and coreference resolution 
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref, entitymentions");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

        // read some text in the text variable
        String text = "John kicked the cart to East New Britain. He went there too.";

        // create an empty Annotation just with the given text
        Annotation document = new Annotation(text);

        // run all Annotators on this text
        pipeline.annotate(document);

        // these are all the sentences in this document
        // a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
        List<CoreMap> sentences = document.get(SentencesAnnotation.class);

        for (CoreMap sentence : sentences) {
            // traversing the words in the current sentence
            // a CoreLabel is a CoreMap with additional token-specific methods
            int count = 1;
            for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
                // this is the text of the token
                String word = token.get(TextAnnotation.class);
                // this is the POS tag of the token
                String pos = token.get(PartOfSpeechAnnotation.class);
                // this is the NER label of the token
                String ne = token.get(NamedEntityTagAnnotation.class);  
                List<CoreMap> entity = token.get(MentionsAnnotation.class);
                System.out.println(word + "-" + count + "/" + pos + "/" + ne + "/" + entity);     
                count++;
            }

            // this is the parse tree of the current sentence
            Tree tree = sentence.get(TreeAnnotation.class);
            
            // this is the Stanford dependency graph of the current sentence
            SemanticGraph dependencies = (SemanticGraph) sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
            System.out.println(dependencies.toList());
            //System.out.println(dependencies.toPOSList());
        }

        // This is the coreference link graph
        // Each chain stores a set of mentions that link to each other,
        // along with a method for getting the most representative mention
        // Both sentence and token offsets start at 1!
        Map<Integer, CorefChain> graph = document.get(CorefChainAnnotation.class);
        for (Integer i : graph.keySet()) {
            CorefChain cc = graph.get(i);
            List<CorefChain.CorefMention> mentions = cc.getMentionsInTextualOrder();
            if (mentions.size() > 1)
                for (CorefChain.CorefMention ment : mentions) 
                    System.out.println(ment.sentNum + " : " + ment.headIndex + " : " + ment.mentionSpan);            
        }
    }
}
